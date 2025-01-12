package com.parallelai.export.implementations;

import com.parallelai.exec.play.GameManager;
import com.parallelai.export.GameStateExporter;
import com.parallelai.export.utilities.GameExporterUtils.*;
import com.parallelai.game.Board;
import com.parallelai.models.utils.Model;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implémentation parallélisée de l'exportateur d'états de jeu.
 * Cette classe propose différentes stratégies de parallélisation pour l'export
 * des données :
 * - Utilisation de ConcurrentHashMap pour la synchronisation
 * - Version sans synchronisation avec fusion finale des résultats
 * - Gestion optimisée de la mémoire avec buffers locaux
 */
public class ParallelExporter extends GameStateExporter {

    /**
     * Crée un nouvel exportateur parallèle.
     *
     * @param outputPath Chemin du fichier de sortie pour les données exportées
     */
    public ParallelExporter(String outputPath) {
        super(outputPath);
    }

    /**
     * Lance plusieurs parties en parallèle avec synchronisation via
     * ConcurrentHashMap.
     * Utilise une barre de progression par thread pour suivre l'avancement.
     *
     * @param nbParties Nombre total de parties à jouer
     * @param model1    Premier modèle (joueur noir)
     * @param model2    Second modèle (joueur blanc)
     * @param nbThreads Nombre de threads à utiliser
     */
    public void startGamesWithUniqueStatesParallel(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads...\n");
        ProgressBar.initDisplay(nbThreads);

        ConcurrentHashMap<String, double[]> globalStateMap = new ConcurrentHashMap<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(nbThreads)) {
            List<Future<Map<String, double[]>>> futures = new ArrayList<>();
            int partiesPerThread = nbParties / nbThreads;

            for (int i = 0; i < nbThreads; i++) {
                int partiesForThisThread = (i == nbThreads - 1) ? partiesPerThread + (nbParties % nbThreads)
                        : partiesPerThread;

                GameThread thread = new GameThread(partiesForThisThread, model1, model2,
                        new ProgressBar(partiesForThisThread, i));

                futures.add(executor.submit(() -> {
                    thread.execute();
                    return thread.getStateMap();
                }));
            }

            // Fusion des résultats
            for (Future<Map<String, double[]>> future : futures) {
                try {
                    future.get().forEach((key, value) -> globalStateMap.merge(key, value, (existing, newVal) -> {
                        existing[65] += newVal[65]; // Somme totale
                        existing[66] += newVal[66]; // Nombre d'occurrences
                        // existing[64] sera recalculé à l'export
                        return existing;
                    }));
                } catch (Exception e) {
                    System.err.println("Erreur thread: " + e.getMessage());
                }
            }

            exportStateMap(globalStateMap);

            System.out.print(String.format("\033[%dH\n", nbThreads + 2));
            System.out.println("Terminé! " + globalStateMap.size() + " situations uniques sauvegardées.");
        }
    }

    /**
     * Thread de jeu avec gestion des collisions via ConcurrentHashMap.
     * Utilise un buffer local pour réduire la fréquence des synchronisations.
     */
    public class GameThread {
        // Constantes
        private static final int BATCH_SIZE = 1000; // Taille du lot pour le traitement par batch

        // Champs de la classe
        private final int nbParties;
        private final Model model1, model2;
        private final ProgressBar progressBar;
        private final ConcurrentHashMap<String, double[]> stateMap;
        private final StateBuffer stateBuffer;
        private Map<String, double[]> localBuffer;

        /**
         * Initialise un nouveau thread de jeu avec synchronisation.
         *
         * @param nbParties   Nombre de parties à jouer dans ce thread
         * @param model1      Premier modèle (joueur noir)
         * @param model2      Second modèle (joueur blanc)
         * @param progressBar Barre de progression associée à ce thread
         */
        public GameThread(int nbParties, Model model1, Model model2, ProgressBar progressBar) {
            this.nbParties = nbParties;
            this.model1 = model1;
            this.model2 = model2;
            this.progressBar = progressBar;
            this.stateMap = new ConcurrentHashMap<>();
            this.stateBuffer = new StateBuffer();
            this.localBuffer = new HashMap<>();
        }

        /**
         * Exécute les parties assignées à ce thread avec gestion par lots.
         * Utilise un buffer local pour accumuler les états avant synchronisation.
         */
        public void execute() {
            // Utiliser un buffer pour accumuler les états avant traitement
            List<GameState> gameStates = new ArrayList<>(BATCH_SIZE);
            int gamesCompleted = 0;

            for (int i = 0; i < nbParties; i++) {
                gameStates.add(processGame());

                if (gameStates.size() >= BATCH_SIZE) {
                    // Traiter d'abord localement
                    processBatchLocally(gameStates);
                    // Synchroniser avec la map globale moins fréquemment
                    synchronizeWithGlobalMap();
                    gameStates.clear();
                }

                gamesCompleted++;
                if (gamesCompleted % 100 == 0) { // Mise à jour moins fréquente de la barre de progression
                    progressBar.update(gamesCompleted);
                }
            }

            if (!gameStates.isEmpty()) {
                processBatchLocally(gameStates);
                synchronizeWithGlobalMap();
            }

            progressBar.update(nbParties);
        }

        /**
         * Traite une partie unique et retourne son historique et résultat.
         */
        private GameState processGame() {
            Board board = new Board();
            GameManager game = new GameManager(board, model1, model2);
            List<CompressedState> history = new ArrayList<>();

            while (game.playNextMove()) {
                history.add(stateBuffer.compressState(board));
            }

            return new GameState(history, calculateGameResult(board));
        }

        /**
         * Traite un lot d'états de jeu localement avant synchronisation.
         * Optimise les performances en réduisant les accès concurrents.
         */
        private void processBatchLocally(List<GameState> gameStates) {
            for (GameState game : gameStates) {
                double finalResult = game.result == 1 ? 1.0 : game.result == 0 ? 0.5 : 0.0;

                for (CompressedState state : game.history) {
                    String key = state.toString();
                    processStateLocally(key, state, finalResult);
                }
            }
        }

        private void processStateLocally(String key, CompressedState state, double finalResult) {
            localBuffer.compute(key, (k, v) -> {
                if (v == null) {
                    double[] newState = state.decompress();
                    newState[64] = 0; // Moyenne (calculée à l'export)
                    newState[65] = finalResult; // Somme totale
                    newState[66] = 1.0; // Nombre d'occurrences
                    return newState;
                } else {
                    v[65] += finalResult; // Ajouter à la somme
                    v[66] += 1.0; // Incrémenter occurrences
                    return v;
                }
            });
        }

        private void synchronizeWithGlobalMap() {
            localBuffer.forEach((key, value) -> stateMap.merge(key, value.clone(), (existing, newVal) -> {
                existing[65] += newVal[65]; // Somme totale
                existing[66] += newVal[66]; // Nombre d'occurrences
                return existing;
            }));
            localBuffer.clear();
        }

        /**
         * Récupère la map d'états de jeu accumulés par ce thread.
         *
         * @return Map contenant les états de jeu uniques et leurs statistiques
         */
        public Map<String, double[]> getStateMap() {
            return stateMap;
        }
    }

    /**
     * Lance plusieurs parties en parallèle sans synchronisation.
     * Chaque thread maintient sa propre map d'états, fusion finale des résultats.
     * Cette approche est plus performante pour un grand nombre de parties.
     */
    public void startGamesWithUniqueStatesParallelNoSync(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println(
                "Début des " + nbParties + " parties avec " + nbThreads + " threads (sans synchronisation)...\n");
        ProgressBar.initDisplay(nbThreads);

        try (ExecutorService executor = Executors.newFixedThreadPool(nbThreads)) {
            List<Future<Map<String, double[]>>> futures = new ArrayList<>();

            // Utiliser un nombre optimal de threads
            int optimalThreads = Math.min(nbThreads, Runtime.getRuntime().availableProcessors());
            int partiesPerThread = nbParties / optimalThreads;

            // Lancer les threads avec leurs propres HashMap
            for (int i = 0; i < nbThreads; i++) {
                int partiesForThisThread = (i == nbThreads - 1) ? partiesPerThread + (nbParties % nbThreads)
                        : partiesPerThread;

                GameThreadNoSync thread = new GameThreadNoSync(partiesForThisThread, model1, model2,
                        new ProgressBar(partiesForThisThread, i));

                futures.add(executor.submit(() -> {
                    thread.execute();
                    return thread.getStateMap();
                }));
            }

            // Fusionner les résultats à la fin
            List<Map<String, double[]>> allResults = new ArrayList<>(nbThreads);
            for (Future<Map<String, double[]>> future : futures) {
                try {
                    allResults.add(future.get());
                } catch (Exception e) {
                    System.err.println("Erreur thread: " + e.getMessage());
                }
            }

            // Utiliser la nouvelle méthode de fusion optimisée
            final Map<String, double[]> finalMap = new HashMap<>();
            streamMerge(allResults, finalMap);

            // Libérer la mémoire explicitement
            allResults.clear();
            allResults = null;
            System.gc();

            exportStateMap(finalMap);

            System.out.print(String.format("\033[%dH\n", nbThreads + 2));
            System.out.println("Terminé! " + finalMap.size() + " situations uniques sauvegardées.");
        }
    }

    /**
     * Thread de jeu sans synchronisation.
     * Maintient tous les états en mémoire locale jusqu'à la fin de l'exécution.
     */
    private class GameThreadNoSync {
        // Constantes
        private static final int BATCH_SIZE = 5000;

        // Champs de la classe
        private final int nbParties;
        private final Model model1, model2;
        private final ProgressBar progressBar;
        private final Map<String, double[]> stateMap;
        private final StateBuffer stateBuffer;

        /**
         * Initialise un nouveau thread de jeu sans synchronisation.
         *
         * @param nbParties   Nombre de parties à jouer dans ce thread
         * @param model1      Premier modèle (joueur noir)
         * @param model2      Second modèle (joueur blanc)
         * @param progressBar Barre de progression associée à ce thread
         */
        public GameThreadNoSync(int nbParties, Model model1, Model model2, ProgressBar progressBar) {
            this.nbParties = nbParties;
            this.model1 = model1;
            this.model2 = model2;
            this.progressBar = progressBar;
            this.stateMap = new HashMap<>(nbParties * 10);
            this.stateBuffer = new StateBuffer();
        }

        /**
         * Exécute les parties assignées à ce thread.
         * Accumule tous les résultats localement sans synchronisation.
         */
        public void execute() {
            List<GameState> gameStates = new ArrayList<>(BATCH_SIZE);
            int gamesCompleted = 0;

            for (int i = 0; i < nbParties; i++) {
                gameStates.add(processGame());

                if (gameStates.size() >= BATCH_SIZE) {
                    processBatchLocal(gameStates);
                    gameStates.clear();
                }

                gamesCompleted++;
                if (gamesCompleted % 100 == 0) {
                    progressBar.update(gamesCompleted);
                }
            }

            if (!gameStates.isEmpty()) {
                processBatchLocal(gameStates);
            }

            progressBar.update(nbParties);
        }

        /**
         * Traite une partie unique et collecte son historique.
         *
         * @return GameState contenant l'historique de la partie et son résultat final
         */
        private GameState processGame() {
            Board board = new Board();
            GameManager game = new GameManager(board, model1, model2);
            List<CompressedState> history = new ArrayList<>();

            while (game.playNextMove()) {
                history.add(stateBuffer.compressState(board));
            }

            return new GameState(history, calculateGameResult(board));
        }

        /**
         * Traite un lot d'états de jeu en local.
         * Met à jour la map d'états avec les nouveaux résultats.
         *
         * @param gameStates Liste des états de jeu à traiter
         */
        private void processBatchLocal(List<GameState> gameStates) {
            for (GameState game : gameStates) {
                double finalResult = game.result == 1 ? 1.0 : game.result == 0 ? 0.5 : 0.0;

                for (CompressedState state : game.history) {
                    String key = state.toString();
                    double[] existing = stateMap.get(key);
                    if (existing == null) {
                        existing = state.decompress();
                        existing[64] = 0; // Moyenne (calculée à l'export)
                        existing[65] = finalResult; // Somme totale
                        existing[66] = 1.0; // Nombre d'occurrences
                        stateMap.put(key, existing);
                    } else {
                        existing[65] += finalResult; // Ajouter à la somme
                        existing[66] += 1.0; // Incrémenter occurrences
                    }
                }
            }
        }

        /**
         * Récupère la map d'états de jeu accumulés par ce thread.
         *
         * @return Map contenant les états de jeu uniques et leurs statistiques
         */
        public Map<String, double[]> getStateMap() {
            return stateMap;
        }
    }

}

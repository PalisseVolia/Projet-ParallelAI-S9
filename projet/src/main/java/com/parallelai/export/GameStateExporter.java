package com.parallelai.export;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Base64;
import java.util.LinkedHashMap;

import com.parallelai.GameManager;
import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.models.MinimaxModel;
import com.parallelai.models.RandomModel;
import com.parallelai.models.utils.Model;

/**
 * Gestionnaire d'export des états de jeu au format CSV.
 * Cette classe permet de:
 * - Sauvegarder l'état du plateau à un moment donné
 * - Convertir les états en format exploitable pour l'apprentissage
 * - Créer un dataset d'entraînement pour le machine learning
 * 
 * Format de sortie:
 * - 64 colonnes pour l'état du plateau (1=noir, -1=blanc, 0=vide)
 * - 1 colonne pour le joueur courant (-1=noir, 1=blanc)
 * - 1 colonne pour le résultat final (-1=défaite, 0=nul, 1=victoire)
 */
public class GameStateExporter {
    /** Chemin du fichier CSV de sortie */
    private final String outputPath;

    /**
     * Crée un nouveau gestionnaire d'export.
     * 
     * @param outputPath Chemin du fichier CSV où sauvegarder les données
     */
    public GameStateExporter(String outputPath) {
        this.outputPath = outputPath;
    }

    /**
     * Exporte l'état actuel du plateau dans le fichier CSV.
     * Convertit chaque case en valeur numérique:
     * - Pion noir = 1
     * - Pion blanc = -1
     * - Case vide = 0
     * Ajoute le résultat final (victoire/défaite) comme dernière colonne.
     * 
     * @param board État du plateau à exporter
     * @param won true si la position est gagnante pour le joueur courant
     */
    public void exportState(Board board, boolean won) {
        try (FileWriter writer = new FileWriter(outputPath, true)) {
            // Écrire l'état du plateau (64 cases)
            Disc[][] grid = board.getGrid();
            StringBuilder line = new StringBuilder();
            
            // Parcourir le plateau et convertir en format CSV
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    // Convertir Disc en valeur numérique
                    int value;
                    if (grid[i][j] == Disc.BLACK) value = 1;
                    else if (grid[i][j] == Disc.WHITE) value = -1;
                    else value = 0;
                    
                    line.append(value);
                    line.append(",");
                }
            }
            
            // Ajouter le résultat (1=victoire, 0=défaite)
            line.append(won ? "1" : "0");
            line.append("\n");
            
            writer.write(line.toString());
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
        }
    }

    /**
     * Exporte une partie complète dans le fichier CSV.
     * Chaque ligne représente un état du jeu.
     * 
     * @param gameStates Liste des états de jeu
     * @param finalBoard Plateau final pour déterminer le gagnant
     */
    public void exportGame(List<Board> gameStates, Board finalBoard) {
        try (FileWriter writer = new FileWriter(outputPath, true)) {
            int finalResult = calculateGameResult(finalBoard);
            boolean isBlackTurn = true; // Le noir commence toujours
            
            // Écrire chaque état du jeu
            for (int i = 0; i < gameStates.size(); i++) {
                Board board = gameStates.get(i);
                
                // Ne sauvegarder que les états correspondant au joueur courant
                if ((i % 2 == 0 && !isBlackTurn) || (i % 2 == 1 && isBlackTurn)) {
                    continue;
                }

                StringBuilder line = new StringBuilder();
                Disc[][] grid = board.getGrid();
                
                // Écriture de l'état du plateau (64 colonnes)
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        int value = grid[row][col] == Disc.BLACK ? 1 : 
                                  grid[row][col] == Disc.WHITE ? -1 : 0;
                        line.append(value).append(",");
                    }
                }
                
                // Ajouter le joueur courant (-1 pour noir, 1 pour blanc)
                line.append(isBlackTurn ? "-1," : "1,");
                
                // Ajouter le résultat final (-1 défaite, 0 nul, 1 victoire)
                if (isBlackTurn) {
                    line.append(finalResult);
                } else {
                    line.append(-finalResult); // Inverser le résultat pour le joueur blanc
                }
                
                line.append("\n");
                writer.write(line.toString());
                
                isBlackTurn = !isBlackTurn; // Alterner les tours
            }
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
        }
    }

   
    /**
     * Joue et exporte plusieurs parties entre deux modèles
     * @param nbParties Nombre de parties à jouer
     * @param model1 Premier modèle (joue les noirs)
     * @param model2 Second modèle (joue les blancs)
     */
    public void startGames(int nbParties, Model model1, Model model2) {
        System.out.println("Début des " + nbParties + " parties...");
        
        for (int i = 0; i < nbParties; i++) {
            // Initialiser une nouvelle partie
            Board board = new Board();
            GameManager game = new GameManager(board, model1, model2);
            
            // Jouer la partie complète
            game.startGame();
            
            if ((i + 1) % 10 == 0) {
                System.out.println("Progression : " + (i + 1) + "/" + nbParties + " parties terminées");
            }
        }
        
        System.out.println("Terminé! " + nbParties + " parties ont été sauvegardées dans " + outputPath);
    }

    public void startGames(int nbParties, boolean save, Model model1, Model model2) {
        System.out.println("Début des " + nbParties + " parties...");
        
        try (FileWriter writer = new FileWriter(outputPath, true)) {
            for (int i = 0; i<nbParties; i++) {
                Board board = new Board();
                GameManager game = new GameManager(board, model1, model2);
                List<Board> gameHistory = new ArrayList<>();
                
                // Jouer et collecter l'historique
                while (game.playNextMove()) {
                    if (save) {
                        gameHistory.add(board.copy());
                    }
                }

                if (save) {
                    // Export optimisé directement avec le writer ouvert
                    exportGameDirectly(gameHistory, board, writer);
                }
                
                if ((i + 1) % 10 == 0) {
                    System.out.println("Progression : " + (i + 1) + "/" + nbParties + " parties terminées");
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
        }
        
        System.out.println("Terminé! " + nbParties + " parties ont été jouées.");
    }

    private void exportGameDirectly(List<Board> gameStates, Board finalBoard, FileWriter writer) throws IOException {
        int finalResult = calculateGameResult(finalBoard);
        boolean isBlackTurn = true;
        
        for (int i = 0; i < gameStates.size(); i++) {
            Board board = gameStates.get(i);
            
            if ((i % 2 == 0 && !isBlackTurn) || (i % 2 == 1 && isBlackTurn)) {
                continue;
            }

            StringBuilder line = new StringBuilder();
            Disc[][] grid = board.getGrid();
            
            // État du plateau
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    int value = grid[row][col] == Disc.BLACK ? 1 : 
                              grid[row][col] == Disc.WHITE ? -1 : 0;
                    line.append(value).append(",");
                }
            }
            
            line.append(isBlackTurn ? "-1," : "1,")
                .append(isBlackTurn ? finalResult : -finalResult)
                .append("\n");
                
            writer.write(line.toString());
            isBlackTurn = !isBlackTurn;
        }
    }

    /**
     * Calcule le résultat final de la partie
     * @param finalBoard Plateau final
     * @return 1 pour victoire des noirs, -1 pour victoire des blancs, 0 pour égalité
     */
    private int calculateGameResult(Board finalBoard) {
        int blackCount = finalBoard.getDiscCount(Disc.BLACK);
        int whiteCount = finalBoard.getDiscCount(Disc.WHITE);
        
        if (blackCount > whiteCount) return 1;
        if (whiteCount > blackCount) return -1;
        return 0;
    }

    /**
     * Joue plusieurs parties et enregistre les situations uniques avec moyennes des résultats
     */
    public void startGamesWithUniqueStates(int nbParties, Model model1, Model model2) {
        System.out.println("Début des " + nbParties + " parties avec situations uniques...");
        
        List<double[]> uniqueStates = new ArrayList<>();
        
        for (int i = 0; i < nbParties; i++) {
            Board board = new Board();
            GameManager game = new GameManager(board, model1, model2);
            List<Board> gameHistory = new ArrayList<>();
            
            // Jouer et collecter l'historique
            while (game.playNextMove()) {
                gameHistory.add(board.copy());
            }
            
            // Calculer le résultat final (1 pour victoire, 0.5 pour nul, 0 pour défaite)
            double finalResult;
            int result = calculateGameResult(board);
            if (result == 1) finalResult = 1.0;      // Victoire
            else if (result == 0) finalResult = 0.5; // Nul
            else finalResult = 0.0;                  // Défaite
            
            // Traiter chaque état du jeu
            for (Board state : gameHistory) {
                double[] currentState = boardToArray(state);
                boolean found = false;
                
                // Chercher si la situation existe déjà
                for (double[] existingState : uniqueStates) {
                    if (isSameState(currentState, existingState)) {
                        existingState[64] += finalResult; // Somme des résultats (1/0.5/0)
                        existingState[65] += 1.0;        // Nombre d'occurrences
                        found = true;
                        break;
                    }
                }
                
                // Si c'est une nouvelle situation, l'ajouter
                if (!found) {
                    currentState[64] = finalResult;  // Premier résultat (1/0.5/0)
                    currentState[65] = 1.0;         // Première occurrence
                    uniqueStates.add(currentState);
                }
            }
            
            if ((i + 1) % 10 == 0) {
                System.out.println("Progression : " + (i + 1) + "/" + nbParties + " parties terminées");
            }
        }
        
        exportUniqueStatesArray(uniqueStates);
        System.out.println("Terminé! " + uniqueStates.size() + " situations uniques sauvegardées.");
    }

    private double[] boardToArray(Board board) {
        double[] state = new double[66]; // 64 cases + somme + occurrences
        Disc[][] grid = board.getGrid();
        
        int index = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                state[index++] = grid[row][col] == Disc.BLACK ? 1 : 
                                grid[row][col] == Disc.WHITE ? -1 : 0;
            }
        }
        
        return state;
    }
    
    private boolean isSameState(double[] state1, double[] state2) {
        // Comparer uniquement les 64 premières valeurs (état du plateau)
        for (int i = 0; i < 64; i++) {
            if (state1[i] != state2[i]) return false;
        }
        return true;
    }
    
    private void exportUniqueStatesArray(List<double[]> uniqueStates) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            for (double[] state : uniqueStates) {
                StringBuilder line = new StringBuilder();
                
                // Écrire l'état du plateau (64 valeurs)
                for (int i = 0; i < 64; i++) {
                    line.append(state[i]).append(",");
                }
                
                // Calculer et écrire la moyenne des résultats
                double average = state[64] / state[65];
                line.append(average).append("\n");
                
                writer.write(line.toString());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
        }
    }

    private List<double[]> mergeThreadResults(List<List<double[]>> threadResults) {
        Map<String, double[]> mergedMap = new HashMap<>();
        
        // Parcourir les résultats de chaque thread
        for (List<double[]> threadResult : threadResults) {
            for (double[] state : threadResult) {
                String key = stateToString(state);
                
                if (mergedMap.containsKey(key)) {
                    double[] existing = mergedMap.get(key);
                    existing[64] += state[64];    // Ajouter les sommes
                    existing[65] += state[65];    // Ajouter les occurrences
                } else {
                    mergedMap.put(key, state.clone());
                }
            }
        }
        
        return new ArrayList<>(mergedMap.values());
    }
    
    private String stateToString(double[] state) {
        StringBuilder sb = new StringBuilder();
        // Uniquement les 64 premiers éléments (état du plateau)
        for (int i = 0; i < 64; i++) {
            sb.append(state[i]).append(",");
        }
        return sb.toString();
    }
    
    private static class ProgressBar {
        private final int total;
        private int current;
        private final int width;
        private final int threadId;
        private static final Object lock = new Object();
        
        public ProgressBar(int total, int threadId) {
            this.total = total;
            this.width = 30; // Barre plus courte pour afficher plusieurs threads
            this.current = 0;
            this.threadId = threadId;
        }
        
        public synchronized void update(int value) {
            // On utilise uniquement la valeur locale du thread, pas le compte global
            this.current = value;
            print();
        }
        
        private void print() {
            synchronized(lock) {
                float percent = (float) current / total;
                int progress = (int) (width * percent);
                
                // Déplacer le curseur à la ligne correspondant au thread
                System.out.print(String.format("\033[%dH\033[K", threadId + 1));
                System.out.print(String.format("Thread %2d [", threadId));
                for (int i = 0; i < width; i++) {
                    if (i < progress) System.out.print("=");
                    else if (i == progress) System.out.print(">");
                    else System.out.print(" ");
                }
                System.out.print(String.format("] %3d%% (%d/%d)", 
                    (int)(percent * 100), current, total));
            }
        }

        public static void initDisplay(int nbThreads) {
            // Effacer l'écran et préparer l'espace pour chaque barre
            System.out.print("\033[2J");  // Effacer l'écran
            System.out.print("\033[H");   // Retour en haut
            for (int i = 0; i < nbThreads; i++) {
                System.out.println();  // Créer une ligne vide pour chaque thread
            }
        }
    }

    private void exportStateMap(Map<String, double[]> stateMap) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            for (double[] state : stateMap.values()) {
                StringBuilder line = new StringBuilder();
                
                // État du plateau
                for (int i = 0; i<64; i++) {
                    line.append(state[i]).append(",");
                }
                
                // Moyenne des résultats
                line.append(state[64] / state[65]).append("\n");
                writer.write(line.toString());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
        }
    }

    // Supprimer la première définition de GameThread et garder uniquement celle-ci
    private class GameThread {
        private final int nbParties;
        private final Model model1, model2;
        private final ProgressBar progressBar;
        private final ConcurrentHashMap<String, double[]> stateMap;
        private final StateBuffer stateBuffer;
        private static final int BATCH_SIZE = 100; // Traiter les états par lots

        public GameThread(int nbParties, Model model1, Model model2, ProgressBar progressBar) {
            this.nbParties = nbParties;
            this.model1 = model1;
            this.model2 = model2;
            this.progressBar = progressBar;
            this.stateMap = new ConcurrentHashMap<>();
            this.stateBuffer = new StateBuffer();
        }

        public void execute() {
            // Utiliser un buffer pour accumuler les états avant traitement
            List<GameState> gameStates = new ArrayList<>(BATCH_SIZE);
            
            for (int i = 0; i < nbParties; i++) {
                gameStates.add(processGame());
                
                if (gameStates.size() >= BATCH_SIZE) {
                    processBatch(gameStates);
                    gameStates.clear();
                }
                
                if ((i + 1) % 10 == 0) progressBar.update(i + 1);
            }
            
            if (!gameStates.isEmpty()) {
                processBatch(gameStates);
            }
            
            progressBar.update(nbParties);
        }

        private GameState processGame() {
            Board board = new Board();
            GameManager game = new GameManager(board, model1, model2);
            List<CompressedState> history = new ArrayList<>();
            
            while (game.playNextMove()) {
                history.add(stateBuffer.compressState(board));
            }

            return new GameState(history, calculateGameResult(board));
        }

        private void processBatch(List<GameState> gameStates) {
            gameStates.parallelStream().forEach(game -> {
                double finalResult = game.result == 1 ? 1.0 : 
                                   game.result == 0 ? 0.5 : 0.0;

                for (CompressedState state : game.history) {
                    String key = state.toString();
                    stateMap.compute(key, (k, v) -> {
                        if (v == null) {
                            double[] newState = state.decompress();
                            newState[64] = finalResult;
                            newState[65] = 1.0;
                            return newState;
                        } else {
                            v[64] += finalResult;
                            v[65] += 1.0;
                            return v;
                        }
                    });
                }
            });
        }

        // Ajouter cette méthode
        public Map<String, double[]> getStateMap() {
            return stateMap;
        }
    }

    // Classe utilitaire pour la compression des états
    private static class StateBuffer {
        private final char[] buffer;
        private final LRUCache<Board, CompressedState> cache;
        
        public StateBuffer() {
            this.buffer = new char[64];
            this.cache = new LRUCache<>(1000); // Cache des 1000 derniers états
        }
        
        public CompressedState compressState(Board board) {
            CompressedState cached = cache.get(board);
            if (cached != null) return cached;
            
            Disc[][] grid = board.getGrid();
            int idx = 0;
            byte[] compressed = new byte[16]; // 64 positions = 16 bytes (4 positions par byte)
            
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    int value = grid[i][j] == Disc.BLACK ? 1 : 
                               grid[i][j] == Disc.WHITE ? 2 : 0;
                    compressed[idx/4] |= (value << ((idx % 4) * 2));
                    idx++;
                }
            }
            
            CompressedState state = new CompressedState(compressed);
            cache.put(board.copy(), state);
            return state;
        }
    }

    // Cache LRU simple
    private static class LRUCache<K,V> extends LinkedHashMap<K,V> {
        private final int maxSize;
        
        public LRUCache(int maxSize) {
            super(16, 0.75f, true);
            this.maxSize = maxSize;
        }
        
        @Override
        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > maxSize;
        }
    }

    // État compressé du plateau
    private static class CompressedState {
        private final byte[] data;
        
        public CompressedState(byte[] data) {
            this.data = data;
        }
        
        public double[] decompress() {
            double[] state = new double[66];
            for (int i = 0; i < 64; i++) {
                int value = (data[i/4] >> ((i % 4) * 2)) & 0x3;
                state[i] = value == 1 ? 1.0 : value == 2 ? -1.0 : 0.0;
            }
            return state;
        }
        
        @Override
        public String toString() {
            return Base64.getEncoder().encodeToString(data);
        }
    }

    // Structure pour regrouper l'historique et le résultat d'une partie
    private static class GameState {
        final List<CompressedState> history;
        final int result;
        
        GameState(List<CompressedState> history, int result) {
            this.history = history;
            this.result = result;
        }
    }

    public void startGamesWithUniqueStatesParallel(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads...\n");
        ProgressBar.initDisplay(nbThreads);
        
        ConcurrentHashMap<String, double[]> globalStateMap = new ConcurrentHashMap<>();
        
        try (ExecutorService executor = Executors.newFixedThreadPool(nbThreads)) {
            List<Future<Map<String, double[]>>> futures = new ArrayList<>();
            int partiesPerThread = nbParties / nbThreads;
            
            for (int i = 0; i < nbThreads; i++) {
                int partiesForThisThread = (i == nbThreads - 1) ? 
                    partiesPerThread + (nbParties % nbThreads) : partiesPerThread;
                    
                GameThread thread = new GameThread(partiesForThisThread, model1, model2, 
                    new ProgressBar(partiesForThisThread, i));
                    
                // Corriger la lambda pour retourner explicitement Map<String, double[]>
                futures.add(executor.submit(() -> {
                    thread.execute();
                    return thread.getStateMap();
                }));
            }
            
            // Fusion des résultats
            for (Future<Map<String, double[]>> future : futures) {
                try {
                    future.get().forEach((key, value) -> 
                        globalStateMap.merge(key, value, (existing, newVal) -> {
                            existing[64] += newVal[64];
                            existing[65] += newVal[65];
                            return existing;
                        })
                    );
                } catch (Exception e) {
                    System.err.println("Erreur thread: " + e.getMessage());
                }
            }
            
            exportStateMap(globalStateMap);
            
            System.out.print(String.format("\033[%dH\n", nbThreads + 2));
            System.out.println("Terminé! " + globalStateMap.size() + " situations uniques sauvegardées.");
        }
    }

    public void startGamesWithUniqueStatesSequential(int nbParties, Model model1, Model model2) {
        System.out.println("Début des " + nbParties + " parties (version séquentielle)...");
        
        Map<String, double[]> stateMap = new HashMap<>();
        StateBuffer stateBuffer = new StateBuffer();
        
        for (int i = 0; i < nbParties; i++) {
            Board board = new Board();
            GameManager game = new GameManager(board, model1, model2);
            List<CompressedState> history = new ArrayList<>();
            
            // Jouer et collecter l'historique
            while (game.playNextMove()) {
                history.add(stateBuffer.compressState(board));
            }
            
            // Calculer le résultat final
            double finalResult;
            int result = calculateGameResult(board);
            finalResult = result == 1 ? 1.0 : result == 0 ? 0.5 : 0.0;
            
            // Traiter chaque état du jeu
            for (CompressedState state : history) {
                String key = state.toString();
                stateMap.compute(key, (k, v) -> {
                    if (v == null) {
                        double[] newState = state.decompress();
                        newState[64] = finalResult;
                        newState[65] = 1.0;
                        return newState;
                    } else {
                        v[64] += finalResult;
                        v[65] += 1.0;
                        return v;
                    }
                });
            }
            
            if ((i + 1) % 100 == 0) {
                System.out.printf("Progression : %d/%d parties terminées (%.1f%%)\n", 
                    i + 1, nbParties, ((i + 1) * 100.0) / nbParties);
            }
        }
        
        exportStateMap(stateMap);
        System.out.println("Terminé! " + stateMap.size() + " situations uniques sauvegardées.");
    }

    public void startGamesWithUniqueStatesParallelNoSync(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (sans synchronisation)...\n");
        ProgressBar.initDisplay(nbThreads);
        
        try (ExecutorService executor = Executors.newFixedThreadPool(nbThreads)) {
            List<Future<Map<String, double[]>>> futures = new ArrayList<>();
            int partiesPerThread = nbParties / nbThreads;
            
            // Lancer les threads avec leurs propres HashMap
            for (int i = 0; i < nbThreads; i++) {
                int partiesForThisThread = (i == nbThreads - 1) ? 
                    partiesPerThread + (nbParties % nbThreads) : partiesPerThread;
                    
                GameThreadNoSync thread = new GameThreadNoSync(partiesForThisThread, model1, model2, 
                    new ProgressBar(partiesForThisThread, i));
                    
                futures.add(executor.submit(() -> {
                    thread.execute();
                    return thread.getStateMap();
                }));
            }
            
            // Fusionner les résultats à la fin
            Map<String, double[]> finalMap = new HashMap<>();
            for (Future<Map<String, double[]>> future : futures) {
                try {
                    Map<String, double[]> threadResult = future.get();
                    // Fusion manuelle des maps
                    threadResult.forEach((key, value) -> 
                        finalMap.merge(key, value, (existing, newVal) -> {
                            double[] merged = existing.clone();
                            merged[64] += newVal[64];
                            merged[65] += newVal[65];
                            return merged;
                        })
                    );
                } catch (Exception e) {
                    System.err.println("Erreur thread: " + e.getMessage());
                }
            }
            
            exportStateMap(finalMap);
            
            System.out.print(String.format("\033[%dH\n", nbThreads + 2));
            System.out.println("Terminé! " + finalMap.size() + " situations uniques sauvegardées.");
        }
    }

    private class GameThreadNoSync {
        private final int nbParties;
        private final Model model1, model2;
        private final ProgressBar progressBar;
        private final Map<String, double[]> stateMap;
        private final StateBuffer stateBuffer;
        private static final int BATCH_SIZE = 100;

        public GameThreadNoSync(int nbParties, Model model1, Model model2, ProgressBar progressBar) {
            this.nbParties = nbParties;
            this.model1 = model1;
            this.model2 = model2;
            this.progressBar = progressBar;
            this.stateMap = new HashMap<>();  // HashMap locale au lieu de ConcurrentHashMap
            this.stateBuffer = new StateBuffer();
        }

        public void execute() {
            List<GameState> gameStates = new ArrayList<>(BATCH_SIZE);
            
            for (int i = 0; i < nbParties; i++) {
                gameStates.add(processGame());
                
                if (gameStates.size() >= BATCH_SIZE) {
                    processBatchLocal(gameStates);
                    gameStates.clear();
                }
                
                if ((i + 1) % 10 == 0) progressBar.update(i + 1);
            }
            
            if (!gameStates.isEmpty()) {
                processBatchLocal(gameStates);
            }
            
            progressBar.update(nbParties);
        }

        private GameState processGame() {
            Board board = new Board();
            GameManager game = new GameManager(board, model1, model2);
            List<CompressedState> history = new ArrayList<>();
            
            while (game.playNextMove()) {
                history.add(stateBuffer.compressState(board));
            }

            return new GameState(history, calculateGameResult(board));
        }

        private void processBatchLocal(List<GameState> gameStates) {
            for (GameState game : gameStates) {
                double finalResult = game.result == 1 ? 1.0 : 
                                   game.result == 0 ? 0.5 : 0.0;

                for (CompressedState state : game.history) {
                    String key = state.toString();
                    processState(key, state, finalResult);
                }
            }
        }

        private void processState(String key, CompressedState state, double finalResult) {
            double[] existing = stateMap.get(key);
            if (existing == null) {
                double[] newState = state.decompress();
                newState[64] = finalResult;
                newState[65] = 1.0;
                stateMap.put(key, newState);
            } else {
                existing[64] += finalResult;
                existing[65] += 1.0;
            }
        }

        public Map<String, double[]> getStateMap() {
            return stateMap;
        }
    }

    public static void main(String[] args) {
        GameStateExporter exporter = new GameStateExporter("projet\\src\\main\\ressources\\data\\game_history.csv");
        
        Model model1 = new MinimaxModel();
        Model model2 = new RandomModel();
         
        long startTime = System.currentTimeMillis();
        int nbParties = 20000;
        int nbThreads = Runtime.getRuntime().availableProcessors();

        System.out.println("Début du test avec " + nbParties + " parties sur " + nbThreads + " threads...");
        
        // Test séquentiel
        // System.out.println("\n=== Test séquentiel ===");
        // long startTimeSeq = System.currentTimeMillis();
        // exporter.startGamesWithUniqueStatesSequential(nbParties, model1, model2);
        // long endTimeSeq = System.currentTimeMillis();
        // double executionTimeSeq = (endTimeSeq - startTimeSeq) / 1000.0;
        
        // Test parallèle
        System.out.println("\n=== Test parallèle (" + nbThreads + " threads) ===");
        long startTimePar = System.currentTimeMillis();
        exporter.startGamesWithUniqueStatesParallel(nbParties, model1, model2, nbThreads);
        long endTimePar = System.currentTimeMillis();
        double executionTimePar = (endTimePar - startTimePar) / 1000.0;

        long startTimePar2 = System.currentTimeMillis();
        exporter.startGamesWithUniqueStatesParallelNoSync(nbParties, model1, model2, nbThreads);
        long endTimePar2 = System.currentTimeMillis();
        double executionTimePar2 = (endTimePar2 - startTimePar2) / 1000.0;
        
        // Affichage des résultats
        System.out.println("\n=== Comparaison des performances ===");
        System.out.printf("Version hasmap : %.2f secondes\n", executionTimePar2);
        System.out.printf("Version sans hashmap   : %.2f secondes\n", executionTimePar);
        // System.out.printf("Accélération       : %.2fx\n", executionTimeSeq / executionTimePar);
    }
}
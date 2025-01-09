package com.parallelai.export.implementations;

import com.parallelai.GameManager;
import com.parallelai.export.GameStateExporter;
import com.parallelai.export.utilities.GameExporterUtils.*;
import com.parallelai.game.Board;
import com.parallelai.models.utils.Model;
import java.util.*;
import java.util.concurrent.*;

public class ParallelExporter extends GameStateExporter {
    
    public ParallelExporter(String outputPath) {
        super(outputPath);
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
                    
                GameThread thread = new GameThread(partiesForThisThread, model1, model2, new ProgressBar(partiesForThisThread, i));
                    
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

    public class GameThread {
        private final int nbParties;
        private final Model model1, model2;
        private final ProgressBar progressBar;
        private final ConcurrentHashMap<String, double[]> stateMap;
        private final StateBuffer stateBuffer;
        private static final int BATCH_SIZE = 1000; 
        private Map<String, double[]> localBuffer; // Buffer local pour réduire la synchronisation

        public GameThread(int nbParties, Model model1, Model model2, ProgressBar progressBar) {
            this.nbParties = nbParties;
            this.model1 = model1;
            this.model2 = model2;
            this.progressBar = progressBar;
            this.stateMap = new ConcurrentHashMap<>();
            this.stateBuffer = new StateBuffer();
            this.localBuffer = new HashMap<>();
        }

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

        private GameState processGame() {
            Board board = new Board();
            GameManager game = new GameManager(board, model1, model2);
            List<CompressedState> history = new ArrayList<>();
            
            while (game.playNextMove()) {
                history.add(stateBuffer.compressState(board));
            }

            return new GameState(history, calculateGameResult(board));
        }

        private void processBatchLocally(List<GameState> gameStates) {
            for (GameState game : gameStates) {
                double finalResult = game.result == 1 ? 1.0 : 
                                   game.result == 0 ? 0.5 : 0.0;

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

        private void synchronizeWithGlobalMap() {
            localBuffer.forEach((key, value) -> 
                stateMap.merge(key, value.clone(), (existing, newVal) -> {
                    existing[64] += newVal[64];
                    existing[65] += newVal[65];
                    return existing;
                })
            );
            localBuffer.clear();
        }

        // Ajouter cette méthode
        public Map<String, double[]> getStateMap() {
            return stateMap;
        }
    }

    // --------------------------------------------------------------------------------------------

    public void startGamesWithUniqueStatesParallelNoSync(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (sans synchronisation)...\n");
        ProgressBar.initDisplay(nbThreads);
        
        try (ExecutorService executor = Executors.newFixedThreadPool(nbThreads)) {
            List<Future<Map<String, double[]>>> futures = new ArrayList<>();
            
            // Utiliser un nombre optimal de threads
            int optimalThreads = Math.min(nbThreads, Runtime.getRuntime().availableProcessors());
            int partiesPerThread = nbParties / optimalThreads;
            
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

    

        private class GameThreadNoSync {
        private final int nbParties;
        private final Model model1, model2;
        private final ProgressBar progressBar;
        private final Map<String, double[]> stateMap;
        private final StateBuffer stateBuffer;
        private static final int BATCH_SIZE = 5000; // Augmenté pour réduire les synchronisations
        @SuppressWarnings("unused")
        private Map<String, double[]> localCache;

        public GameThreadNoSync(int nbParties, Model model1, Model model2, ProgressBar progressBar) {
            this.nbParties = nbParties;
            this.model1 = model1;
            this.model2 = model2;
            this.progressBar = progressBar;
            this.stateMap = new HashMap<>(nbParties * 10); // Préallocation plus grande
            this.stateBuffer = new StateBuffer();
            this.localCache = new HashMap<>(BATCH_SIZE * 100); // Pré-allouer une grande taille
        }

        public void execute() {
            List<GameState> gameStates = new ArrayList<>(BATCH_SIZE);
            int gamesCompleted = 0;
            
            for (int i = 0; i<nbParties; i++) {
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
                    double[] existing = stateMap.get(key);
                    if (existing == null) {
                        existing = state.decompress();
                        existing[64] = finalResult;
                        existing[65] = 1.0;
                        stateMap.put(key, existing);
                    } else {
                        existing[64] += finalResult;
                        existing[65] += 1.0;
                    }
                }
            }
        }

        public Map<String, double[]> getStateMap() {
            return stateMap;
        }
    }

}

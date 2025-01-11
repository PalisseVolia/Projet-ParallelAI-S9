package com.parallelai.export.implementations;

import com.parallelai.exec.play.GameManager;
import com.parallelai.export.GameStateExporter;
import com.parallelai.export.utilities.GameExporterUtils.*;
import com.parallelai.game.Board;
import com.parallelai.models.utils.Model;
import com.parallelai.players.AIPlayer;
import com.parallelai.players.AIWeightedPlayer;

import java.util.*;

/**
 * Implémentation classique de l'exporteur de jeux utilisant des threads Java standards.
 * Cette classe permet de :
 * - Jouer plusieurs parties en parallèle
 * - Sauvegarder ou non les états de jeu
 * - Gérer différents types de joueurs (Models, AIPlayers, AIWeightedPlayers)
 * - Optimiser les performances avec un système de batch
 */
public class ClassicThreadExporter extends GameStateExporter {
    // Ajout des compteurs pour les statistiques
    private class GameStats {
        int blackWins = 0;
        int whiteWins = 0;
        int draws = 0;
    }

    /**
     * Constructeur de l'exporteur
     * @param outputPath Chemin du fichier de sortie (peut être null si pas de sauvegarde)
     */
    public ClassicThreadExporter(String outputPath) {
        super(outputPath);
    }

    /**
     * Lance plusieurs parties en parallèle avec des Models et sauvegarde les états uniques
     * @param nbParties Nombre total de parties à jouer
     * @param model1 Premier modèle (joueur noir)
     * @param model2 Second modèle (joueur blanc)
     * @param nbThreads Nombre de threads à utiliser
     * @param appendToExisting Si true, ajoute aux données existantes
     */
    public void startGamesWithUniqueStatesClassicThreads(int nbParties, Model model1, Model model2, int nbThreads, boolean appendToExisting) {
        Map<String, double[]> existingData = new HashMap<>();
        GameStats globalStats = new GameStats();
        
        // Charger les données existantes seulement si demandé
        if (appendToExisting) {
            System.out.println("Chargement des données existantes...");
            existingData = loadExistingCSV();
            System.out.println(existingData.size() + " états déjà existants chargés.");
        }
        
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (version classique)...\n");
        ProgressBar.initDisplay(nbThreads);

        Thread[] threads = new Thread[nbThreads];
        @SuppressWarnings("unchecked")
        Map<String, double[]>[] threadResults = new HashMap[nbThreads];
        ProgressBar[] progressBars = new ProgressBar[nbThreads];
        
        int partiesPerThread = nbParties / nbThreads;
        final int BATCH_SIZE = 1000;

        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            threadResults[i] = new HashMap<>();
            final Map<String, double[]> localStateMap = threadResults[i];
            final StateBuffer stateBuffer = new StateBuffer();
            
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;

            progressBars[i] = new ProgressBar(partiesForThisThread, threadId);

            threads[i] = new Thread(() -> {
                int gamesCompleted = 0;
                List<GameState> batchBuffer = new ArrayList<>(BATCH_SIZE);
                GameStats threadStats = new GameStats();
                
                for (int game = 0; game < partiesForThisThread; game++) {
                    Board board = new Board();
                    GameManager gameManager = new GameManager(board, model1, model2);
                    List<CompressedState> history = new ArrayList<>();

                    while (gameManager.playNextMove()) {
                        history.add(stateBuffer.compressState(board));
                    }

                    int result = calculateGameResult(board);
                    double finalResult = result == 1 ? 1.0 : result == 0 ? 0.5 : 0.0;
                    
                    batchBuffer.add(new GameState(history, result));
                    
                    if (batchBuffer.size() >= BATCH_SIZE) {
                        processBatchLocal(batchBuffer, localStateMap, finalResult);
                        batchBuffer.clear();
                    }

                    synchronized(globalStats) {
                        updateStats(globalStats, result);
                    }

                    gamesCompleted++;
                    if (gamesCompleted % 100 == 0) {
                        progressBars[threadId].update(gamesCompleted);
                    }
                }
                
                if (!batchBuffer.isEmpty()) {
                    processBatchLocal(batchBuffer, localStateMap, 0.0);
                }
                
                progressBars[threadId].update(partiesForThisThread);
            });

            threads[i].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Interruption pendant l'attente des threads");
            Thread.currentThread().interrupt();
            return;
        }

        // Move cursor below progress bars
        System.out.print(String.format("\033[%dH\n", nbThreads + 2));

        List<Map<String, double[]>> allResults = new ArrayList<>(nbThreads);
        for (int i = 0; i < threadResults.length; i++) {
            allResults.add(threadResults[i]);
            threadResults[i] = null; // Libérer la mémoire immédiatement
        }

        Map<String, double[]> finalMap = new HashMap<>();
        streamMerge(allResults, finalMap);

        if (appendToExisting) {
            // Fusion intelligente des données existantes avec les nouvelles
            existingData.forEach((key, existingValue) -> {
                finalMap.compute(key, (k, newValue) -> {
                    if (newValue == null) {
                        return existingValue;
                    } else {
                        // Mettre à jour les compteurs et moyennes
                        newValue[64] += existingValue[64]; // Somme des résultats
                        newValue[65] += existingValue[65]; // Nombre d'occurrences
                        return newValue;
                    }
                });
            });
        }

        // Libérer la mémoire
        allResults.clear();
        allResults = null;
        System.gc();

        exportStateMap(finalMap);
        System.out.println("Terminé! " + finalMap.size() + " situations uniques sauvegardées.");
        displayGameStats(globalStats);
    }

    /**
     * Lance plusieurs parties en parallèle avec des AIPlayers et sauvegarde les états uniques
     * Surcharge adaptée aux joueurs de type AIPlayer
     * @param nbParties Nombre total de parties à jouer
     * @param player1 Premier joueur (noir)
     * @param player2 Second joueur (blanc)
     * @param nbThreads Nombre de threads à utiliser
     * @param appendToExisting Si true, ajoute aux données existantes
     */
    public void startGamesWithUniqueStatesClassicThreads(int nbParties, AIPlayer player1, AIPlayer player2, int nbThreads, boolean appendToExisting) {
        Map<String, double[]> existingData = new HashMap<>();
        GameStats globalStats = new GameStats();
        
        // Charger les données existantes seulement si demandé
        if (appendToExisting) {
            System.out.println("Chargement des données existantes...");
            existingData = loadExistingCSV();
            System.out.println(existingData.size() + " états déjà existants chargés.");
        }
        
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (version classique)...\n");
        ProgressBar.initDisplay(nbThreads);

        Thread[] threads = new Thread[nbThreads];
        @SuppressWarnings("unchecked")
        Map<String, double[]>[] threadResults = new HashMap[nbThreads];
        ProgressBar[] progressBars = new ProgressBar[nbThreads];
        
        int partiesPerThread = nbParties / nbThreads;
        final int BATCH_SIZE = 1000;

        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            threadResults[i] = new HashMap<>();
            final Map<String, double[]> localStateMap = threadResults[i];
            final StateBuffer stateBuffer = new StateBuffer();
            
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;

            progressBars[i] = new ProgressBar(partiesForThisThread, threadId);

            threads[i] = new Thread(() -> {
                int gamesCompleted = 0;
                List<GameState> batchBuffer = new ArrayList<>(BATCH_SIZE);
                GameStats threadStats = new GameStats();
                
                for (int game = 0; game < partiesForThisThread; game++) {
                    Board board = new Board();
                    GameManager gameManager = new GameManager(board, player1.model, player2.model);
                    List<CompressedState> history = new ArrayList<>();

                    while (gameManager.playNextMove()) {
                        history.add(stateBuffer.compressState(board));
                    }

                    int result = calculateGameResult(board);
                    batchBuffer.add(new GameState(history, result));
                    
                    if (batchBuffer.size() >= BATCH_SIZE) {
                        processBatchLocal(batchBuffer, localStateMap, 0.0);
                        batchBuffer.clear();
                    }

                    synchronized(globalStats) {
                        updateStats(globalStats, result);
                    }

                    gamesCompleted++;
                    if (gamesCompleted % 100 == 0) {
                        progressBars[threadId].update(gamesCompleted);
                    }
                }
                
                if (!batchBuffer.isEmpty()) {
                    processBatchLocal(batchBuffer, localStateMap, 0.0);
                }
                
                progressBars[threadId].update(partiesForThisThread);
            });

            threads[i].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Interruption pendant l'attente des threads");
            Thread.currentThread().interrupt();
            return;
        }

        // Move cursor below progress bars
        System.out.print(String.format("\033[%dH\n", nbThreads + 2));

        List<Map<String, double[]>> allResults = new ArrayList<>(nbThreads);
        for (int i = 0; i < threadResults.length; i++) {
            allResults.add(threadResults[i]);
            threadResults[i] = null; // Libérer la mémoire immédiatement
        }

        Map<String, double[]> finalMap = new HashMap<>();
        streamMerge(allResults, finalMap);

        if (appendToExisting) {
            // Fusion intelligente des données existantes avec les nouvelles
            existingData.forEach((key, existingValue) -> {
                finalMap.compute(key, (k, newValue) -> {
                    if (newValue == null) {
                        return existingValue;
                    } else {
                        // Mettre à jour les compteurs et moyennes
                        newValue[64] += existingValue[64]; // Somme des résultats
                        newValue[65] += existingValue[65]; // Nombre d'occurrences
                        return newValue;
                    }
                });
            });
        }

        // Libérer la mémoire
        allResults.clear();
        allResults = null;
        System.gc();

        exportStateMap(finalMap);
        System.out.println("Terminé! " + finalMap.size() + " situations uniques sauvegardées.");
        displayGameStats(globalStats);
    }

     /**
     * Lance plusieurs parties en parallèle avec des AIWeightedPlayers et sauvegarde les états uniques
     * Surcharge adaptée aux joueurs utilisant des poids
     * @param nbParties Nombre total de parties à jouer
     * @param player1 Premier joueur pondéré (noir)
     * @param player2 Second joueur pondéré (blanc)
     * @param nbThreads Nombre de threads à utiliser
     * @param appendToExisting Si true, ajoute aux données existantes
     */
    public void startGamesWithUniqueStatesClassicThreads(int nbParties, AIWeightedPlayer player1, AIWeightedPlayer player2, int nbThreads, boolean appendToExisting) {
        Map<String, double[]> existingData = new HashMap<>();
        GameStats globalStats = new GameStats();
        
        // Charger les données existantes seulement si demandé
        if (appendToExisting) {
            System.out.println("Chargement des données existantes...");
            existingData = loadExistingCSV();
            System.out.println(existingData.size() + " états déjà existants chargés.");
        }
        
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (version classique)...\n");
        ProgressBar.initDisplay(nbThreads);

        Thread[] threads = new Thread[nbThreads];
        @SuppressWarnings("unchecked")
        Map<String, double[]>[] threadResults = new HashMap[nbThreads];
        ProgressBar[] progressBars = new ProgressBar[nbThreads];
        
        int partiesPerThread = nbParties / nbThreads;
        final int BATCH_SIZE = 1000;

        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            threadResults[i] = new HashMap<>();
            final Map<String, double[]> localStateMap = threadResults[i];
            final StateBuffer stateBuffer = new StateBuffer();
            
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;

            progressBars[i] = new ProgressBar(partiesForThisThread, threadId);

            threads[i] = new Thread(() -> {
                int gamesCompleted = 0;
                List<GameState> batchBuffer = new ArrayList<>(BATCH_SIZE);
                GameStats threadStats = new GameStats();
                
                for (int game = 0; game < partiesForThisThread; game++) {
                    Board board = new Board();
                    GameManager gameManager = new GameManager(board, player1.model, player2.model);
                    List<CompressedState> history = new ArrayList<>();

                    while (gameManager.playNextMove()) {
                        history.add(stateBuffer.compressState(board));
                    }

                    int result = calculateGameResult(board);
                    batchBuffer.add(new GameState(history, result));
                    
                    if (batchBuffer.size() >= BATCH_SIZE) {
                        processBatchLocal(batchBuffer, localStateMap, 0.0);
                        batchBuffer.clear();
                    }

                    synchronized(globalStats) {
                        updateStats(globalStats, result);
                    }

                    gamesCompleted++;
                    if (gamesCompleted % 100 == 0) {
                        progressBars[threadId].update(gamesCompleted);
                    }
                }
                
                if (!batchBuffer.isEmpty()) {
                    processBatchLocal(batchBuffer, localStateMap, 0.0);
                }
                
                progressBars[threadId].update(partiesForThisThread);
            });

            threads[i].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Interruption pendant l'attente des threads");
            Thread.currentThread().interrupt();
            return;
        }

        // Move cursor below progress bars
        System.out.print(String.format("\033[%dH\n", nbThreads + 2));

        List<Map<String, double[]>> allResults = new ArrayList<>(nbThreads);
        for (int i = 0; i < threadResults.length; i++) {
            allResults.add(threadResults[i]);
            threadResults[i] = null; // Libérer la mémoire immédiatement
        }

        Map<String, double[]> finalMap = new HashMap<>();
        streamMerge(allResults, finalMap);

        if (appendToExisting) {
            // Fusion intelligente des données existantes avec les nouvelles
            existingData.forEach((key, existingValue) -> {
                finalMap.compute(key, (k, newValue) -> {
                    if (newValue == null) {
                        return existingValue;
                    } else {
                        // Mettre à jour les compteurs et moyennes
                        newValue[64] += existingValue[64]; // Somme des résultats
                        newValue[65] += existingValue[65]; // Nombre d'occurrences
                        return newValue;
                    }
                });
            });
        }

        // Libérer la mémoire
        allResults.clear();
        allResults = null;
        System.gc();

        exportStateMap(finalMap);
        System.out.println("Terminé! " + finalMap.size() + " situations uniques sauvegardées.");
        displayGameStats(globalStats);
    }

    /**
     * Joue plusieurs parties en parallèle sans sauvegarder les états (pour les Models)
     * Utile pour les tests de performance ou l'entraînement sans sauvegarde
     * @param nbParties Nombre de parties à jouer
     * @param model1 Premier modèle
     * @param model2 Second modèle
     * @param nbThreads Nombre de threads à utiliser
     */
    public void startGamesNoSave(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (sans sauvegarde)...\n");
        ProgressBar.initDisplay(nbThreads);
        GameStats globalStats = new GameStats();

        Thread[] threads = new Thread[nbThreads];
        int partiesPerThread = nbParties / nbThreads;

        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;

            ProgressBar progressBar = new ProgressBar(partiesForThisThread, threadId);

            threads[i] = new Thread(() -> {
                int gamesCompleted = 0;
                GameStats threadStats = new GameStats();
                
                for (int game = 0; game < partiesForThisThread; game++) {
                    Board board = new Board();
                    GameManager gameManager = new GameManager(board, model1, model2);
                    
                    // Jouer la partie jusqu'à la fin sans sauvegarder
                    while (gameManager.playNextMove()) {
                        // Continue jusqu'à la fin de la partie
                    }
                    
                    int result = calculateGameResult(board);
                    synchronized(globalStats) {
                        updateStats(globalStats, result);
                    }

                    gamesCompleted++;
                    if (gamesCompleted % 100 == 0) {
                        progressBar.update(gamesCompleted);
                    }
                }
                
                progressBar.update(partiesForThisThread);
            });

            threads[i].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Interruption pendant l'attente des threads");
            Thread.currentThread().interrupt();
            return;
        }

        // Move cursor below progress bars
        System.out.print(String.format("\033[%dH\n", nbThreads + 2));
        System.out.println("Terminé! " + nbParties + " parties ont été jouées.");
        displayGameStats(globalStats);
    }

    /**
     * Joue plusieurs parties en parallèle sans sauvegarder les états (pour les AIPlayers)
     * @param nbParties Nombre de parties à jouer
     * @param player1 Premier joueur AI
     * @param player2 Second joueur AI
     * @param nbThreads Nombre de threads à utiliser
     */
    public void startGamesNoSave(int nbParties, AIPlayer player1, AIPlayer player2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (sans sauvegarde)...\n");
        ProgressBar.initDisplay(nbThreads);
        GameStats globalStats = new GameStats();

        Thread[] threads = new Thread[nbThreads];
        int partiesPerThread = nbParties / nbThreads;

        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;

            ProgressBar progressBar = new ProgressBar(partiesForThisThread, threadId);

            threads[i] = new Thread(() -> {
                int gamesCompleted = 0;
                GameStats threadStats = new GameStats();
                
                for (int game = 0; game < partiesForThisThread; game++) {
                    Board board = new Board();
                    GameManager gameManager = new GameManager(board, player1.model, player2.model);
                    
                    // Jouer la partie jusqu'à la fin sans sauvegarder
                    while (gameManager.playNextMove()) {
                        // Continue jusqu'à la fin de la partie
                    }
                    
                    int result = calculateGameResult(board);
                    synchronized(globalStats) {
                        updateStats(globalStats, result);
                    }

                    gamesCompleted++;
                    if (gamesCompleted % 100 == 0) {
                        progressBar.update(gamesCompleted);
                    }
                }
                
                progressBar.update(partiesForThisThread);
            });

            threads[i].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Interruption pendant l'attente des threads");
            Thread.currentThread().interrupt();
            return;
        }

        System.out.print(String.format("\033[%dH\n", nbThreads + 2));
        System.out.println("Terminé! " + nbParties + " parties ont été jouées.");
        displayGameStats(globalStats);
    }

    /**
     * Joue plusieurs parties en parallèle sans sauvegarder les états (pour les AIWeightedPlayers)
     * @param nbParties Nombre de parties à jouer
     * @param player1 Premier joueur AI pondéré
     * @param player2 Second joueur AI pondéré
     * @param nbThreads Nombre de threads à utiliser
     */
    public void startGamesNoSave(int nbParties, AIWeightedPlayer player1, AIWeightedPlayer player2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (sans sauvegarde)...\n");
        ProgressBar.initDisplay(nbThreads);
        GameStats globalStats = new GameStats();

        Thread[] threads = new Thread[nbThreads];
        int partiesPerThread = nbParties / nbThreads;

        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;

            ProgressBar progressBar = new ProgressBar(partiesForThisThread, threadId);

            threads[i] = new Thread(() -> {
                int gamesCompleted = 0;
                GameStats threadStats = new GameStats();
                
                for (int game = 0; game < partiesForThisThread; game++) {
                    Board board = new Board();
                    GameManager gameManager = new GameManager(board, player1.model, player2.model);
                    
                    // Jouer la partie jusqu'à la fin sans sauvegarder
                    while (gameManager.playNextMove()) {
                        // Continue jusqu'à la fin de la partie
                    }
                    
                    int result = calculateGameResult(board);
                    synchronized(globalStats) {
                        updateStats(globalStats, result);
                    }

                    gamesCompleted++;
                    if (gamesCompleted % 100 == 0) {
                        progressBar.update(gamesCompleted);
                    }
                }
                
                progressBar.update(partiesForThisThread);
            });

            threads[i].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Interruption pendant l'attente des threads");
            Thread.currentThread().interrupt();
            return;
        }

        System.out.print(String.format("\033[%dH\n", nbThreads + 2));
        System.out.println("Terminé! " + nbParties + " parties ont été jouées.");
        displayGameStats(globalStats);
    }

    /**
     * Traite un lot d'états de jeu pour les stocker dans la map locale
     * Optimise les performances en traitant plusieurs états à la fois
     * @param batch Liste des états de jeu à traiter
     * @param localMap Map locale du thread pour stocker les états
     * @param finalResult Résultat final de la partie
     */
    private void processBatchLocal(List<GameState> batch, Map<String, double[]> localMap, double finalResult) {
        for (GameState game : batch) {
            for (CompressedState state : game.history) {
                String key = state.toString();
                processStateLocal(localMap, key, state, finalResult);
            }
        }
    }

    /**
     * Traite un état individuel et l'ajoute à la map locale
     * Gère la création de nouveaux états ou la mise à jour des existants
     * @param localMap Map locale du thread
     * @param key Clé unique de l'état
     * @param state État compressé à traiter
     * @param finalResult Résultat final associé à cet état
     */
    private void processStateLocal(Map<String, double[]> localMap, String key, CompressedState state, double finalResult) {
        double[] existing = localMap.get(key);
        if (existing == null) {
            double[] newState = state.decompress();
            // Vérification de sécurité
            if (newState.length != 67) {
                double[] resizedState = new double[67];
                System.arraycopy(newState, 0, resizedState, 0, Math.min(newState.length, 67));
                newState = resizedState;
            }
            newState[64] = 0; // moyenne (calculée à l'export)
            newState[65] = finalResult; // somme totale
            newState[66] = 1.0; // nombre d'occurrences
            localMap.put(key, newState);
        } else {
            existing[65] += finalResult; // ajouter à la somme
            existing[66] += 1.0; // incrémenter le compteur
        }
    }

    private void displayGameStats(GameStats stats) {
        System.out.println("\nRésultats finaux :");
        System.out.println("Victoires des Noirs : " + stats.blackWins);
        System.out.println("Victoires des Blancs : " + stats.whiteWins);
        System.out.println("Matchs nuls : " + stats.draws);
    }

    private void updateStats(GameStats stats, int result) {
        if (result == 1) stats.blackWins++;
        else if (result == -1) stats.whiteWins++;
        else stats.draws++;
    }
}


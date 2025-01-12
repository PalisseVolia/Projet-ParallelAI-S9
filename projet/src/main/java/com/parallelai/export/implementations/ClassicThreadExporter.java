package com.parallelai.export.implementations;

import com.parallelai.exec.play.GameManager;
import com.parallelai.export.GameStateExporter;
import com.parallelai.export.utilities.GameExporterUtils.*;
import com.parallelai.game.Board;
import com.parallelai.models.RandomModel;
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
            Map<String, double[]> mergedMap = new HashMap<>(existingData); // Copie les données existantes
            
            finalMap.forEach((key, newValue) -> {
                mergedMap.compute(key, (k, existingValue) -> {
                    if (existingValue == null) {
                        return newValue.clone(); // Nouvelle donnée
                    } else {
                        // Mettre à jour les compteurs et moyennes
                        existingValue[65] += newValue[65]; // Somme totale
                        existingValue[66] += newValue[66]; // Nombre d'occurrences
                        // existingValue[64] sera recalculé à l'export
                        return existingValue;
                    }
                });
            });
            
            finalMap = mergedMap; // Remplace finalMap par la version fusionnée
        }

        // Libérer la mémoire
        allResults.clear();
        allResults = null;

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
            Map<String, double[]> mergedMap = new HashMap<>(existingData); // Copie les données existantes
            
            finalMap.forEach((key, newValue) -> {
                mergedMap.compute(key, (k, existingValue) -> {
                    if (existingValue == null) {
                        return newValue.clone(); // Nouvelle donnée
                    } else {
                        // Mettre à jour les compteurs et moyennes
                        existingValue[65] += newValue[65]; // Somme totale
                        existingValue[66] += newValue[66]; // Nombre d'occurrences
                        // existingValue[64] sera recalculé à l'export
                        return existingValue;
                    }
                });
            });
            
            finalMap = mergedMap; // Remplace finalMap par la version fusionnée
        }

        // Libérer la mémoire
        allResults.clear();
        allResults = null;

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
            Map<String, double[]> mergedMap = new HashMap<>(existingData); // Copie les données existantes
            
            finalMap.forEach((key, newValue) -> {
                mergedMap.compute(key, (k, existingValue) -> {
                    if (existingValue == null) {
                        return newValue.clone(); // Nouvelle donnée
                    } else {
                        // Mettre à jour les compteurs et moyennes
                        existingValue[65] += newValue[65]; // Somme totale
                        existingValue[66] += newValue[66]; // Nombre d'occurrences
                        // existingValue[64] sera recalculé à l'export
                        return existingValue;
                    }
                });
            });
            
            finalMap = mergedMap; // Remplace finalMap par la version fusionnée
        }

        // Libérer la mémoire
        allResults.clear();
        allResults = null;

        exportStateMap(finalMap);
        System.out.println("Terminé! " + finalMap.size() + " situations uniques sauvegardées.");
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
            double gameResult = game.result == 1 ? 1.0 : game.result == 0 ? 0.5 : 0.0;
            
            for (CompressedState state : game.history) {
                String key = state.toString();
                processStateLocal(localMap, key, state, gameResult);  // Utiliser le résultat de la partie
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

    public static void main(String[] args) {
        // Paramètres de test
        int nbParties = 100;
        int nbThreads = Runtime.getRuntime().availableProcessors();
        String outputPath = "test_dataset_classic.csv";
        
        // Création des modèles pour le test
        Model model1 = new RandomModel();
        Model model2 = new RandomModel();
        
        System.out.println("=== Test sans fichier existant ===");
        ClassicThreadExporter exporter = new ClassicThreadExporter(outputPath);
        exporter.startGamesWithUniqueStatesClassicThreads(nbParties, model1, model2, nbThreads, false);
        
        // Récupérer le nombre de situations de la première exécution
        int firstRunSize = 0;
        try {
            Map<String, double[]> firstRunData = exporter.loadExistingCSV();
            firstRunSize = firstRunData.size();
            System.out.println("\nNombre de situations après première exécution : " + firstRunSize);
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du premier fichier");
        }
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n=== Test avec fichier existant ===");
        ClassicThreadExporter exporter2 = new ClassicThreadExporter(outputPath);
        exporter2.startGamesWithUniqueStatesClassicThreads(nbParties, model1, model2, nbThreads, true);
        
        // Vérifier le nombre final de situations
        try {
            Map<String, double[]> finalData = exporter2.loadExistingCSV();
            System.out.println("\nNombre de situations après fusion : " + finalData.size());
            System.out.println("Différence : " + (finalData.size() - firstRunSize) + " nouvelles situations");
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du fichier final");
        }
    }
}


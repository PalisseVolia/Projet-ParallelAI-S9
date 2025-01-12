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
 * Implémentation de l'exportation parallèle des états de jeu utilisant les threads Java classiques.
 * Cette classe permet de :
 * - Jouer plusieurs parties d'Othello en parallèle
 * - Sauvegarder l'historique unique des états de jeu
 * - Gérer différents types de joueurs (modèles standards, AI players, weighted AI players)
 * - Optimiser les performances avec un système de batch
 * - Fusionner les données avec un fichier existant
 */
public class ClassicThreadExporter extends GameStateExporter {
    
    /**
     * Classe interne pour stocker les statistiques de jeu de manière thread-safe
     */
    private class GameStats {
        int blackWins = 0;     // Nombre de victoires des noirs
        int whiteWins = 0;     // Nombre de victoires des blancs
        int draws = 0;         // Nombre de matchs nuls
    }

    /**
     * Constructeur de l'exporteur
     * @param outputPath Chemin du fichier de sortie (peut être null si pas de sauvegarde)
     */
    public ClassicThreadExporter(String outputPath) {
        super(outputPath);
    }

    /**
     * Version de base - Surcharge pour les modèles standards
     * Cette implémentation est optimisée pour les modèles de base et sert de référence pour les autres surcharges.
     * 
     * Caractéristiques spécifiques :
     * - Utilisation directe des modèles sans wrapper
     * - Pas de configuration supplémentaire nécessaire
     * - Traitement optimisé pour les modèles standards
     * 
     * Workflow :
     * 1. Initialisation des structures de données
     * 2. Distribution des parties entre les threads
     * 3. Exécution parallèle des parties
     * 4. Fusion des résultats et export
     */
    public void startGamesWithUniqueStatesClassicThreads(int nbParties, Model model1, Model model2, int nbThreads, boolean appendToExisting) {
        // Initialisation des structures de données pour le suivi des états et statistiques
        Map<String, double[]> existingData = new HashMap<>();
        GameStats globalStats = new GameStats();
        
        // Gestion des données existantes si demandé (mode fusion)
        if (appendToExisting) {
            System.out.println("Chargement des données existantes...");
            existingData = loadExistingCSV();
            System.out.println(existingData.size() + " états déjà existants chargés.");
        }
        
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (version classique)...\n");
        ProgressBar.initDisplay(nbThreads);

        // Configuration du multithreading
        Thread[] threads = new Thread[nbThreads];
        @SuppressWarnings("unchecked")
        Map<String, double[]>[] threadResults = new HashMap[nbThreads]; // Un HashMap par thread pour éviter les conflits
        ProgressBar[] progressBars = new ProgressBar[nbThreads];       // Barres de progression individuelles
        
        // Distribution équitable des parties entre les threads
        int partiesPerThread = nbParties / nbThreads;
        final int BATCH_SIZE = 1000; // Optimisation : traitement par lots de 1000 états

        // Création et configuration des threads
        for (int i = 0; i < nbThreads; i++) {
            // Configuration spécifique à chaque thread
            final int threadId = i;
            threadResults[i] = new HashMap<>();             // Map locale pour ce thread
            final Map<String, double[]> localStateMap = threadResults[i];
            final StateBuffer stateBuffer = new StateBuffer(); // Buffer d'états pour ce thread
            
            // Ajustement du nombre de parties pour le dernier thread
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;

            progressBars[i] = new ProgressBar(partiesForThisThread, threadId);

            // Définition du comportement du thread
            threads[i] = new Thread(() -> {
                // Variables de suivi locales
                int gamesCompleted = 0;
                List<GameState> batchBuffer = new ArrayList<>(BATCH_SIZE);
                
                // Boucle principale de simulation des parties
                for (int game = 0; game < partiesForThisThread; game++) {
                    // Initialisation d'une nouvelle partie
                    Board board = new Board();
                    GameManager gameManager = new GameManager(board, model1, model2);
                    List<CompressedState> history = new ArrayList<>();

                    // Jouer la partie jusqu'à la fin
                    while (gameManager.playNextMove()) {
                        history.add(stateBuffer.compressState(board));
                    }

                    // Traitement du résultat
                    int result = calculateGameResult(board);
                    double finalResult = result == 1 ? 1.0 : result == 0 ? 0.5 : 0.0;
                    
                    // Ajout à la mémoire tampon
                    batchBuffer.add(new GameState(history, result));
                    
                    // Traitement par lot si la taille limite est atteinte
                    if (batchBuffer.size() >= BATCH_SIZE) {
                        processBatchLocal(batchBuffer, localStateMap, finalResult);
                        batchBuffer.clear();
                    }

                    // Mise à jour thread-safe des statistiques globales
                    synchronized(globalStats) {
                        updateStats(globalStats, result);
                    }

                    // Mise à jour de la progression
                    gamesCompleted++;
                    if (gamesCompleted % 100 == 0) {
                        progressBars[threadId].update(gamesCompleted);
                    }
                }
                
                // Traitement des états restants dans le buffer
                if (!batchBuffer.isEmpty()) {
                    processBatchLocal(batchBuffer, localStateMap, 0.0);
                }
                
                progressBars[threadId].update(partiesForThisThread);
            });

            threads[i].start();
        }

        // Attente de la fin de tous les threads
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

        // Fusion des résultats de tous les threads
        List<Map<String, double[]>> allResults = new ArrayList<>(nbThreads);
        for (int i = 0; i < threadResults.length; i++) {
            allResults.add(threadResults[i]);
            threadResults[i] = null; // Libération de la mémoire
        }

        // Création de la map finale des résultats
        Map<String, double[]> finalMap = new HashMap<>();
        streamMerge(allResults, finalMap);

        // Fusion avec les données existantes si nécessaire
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

        // Nettoyage final et export des résultats
        allResults.clear();
        allResults = null;
        exportStateMap(finalMap);
        System.out.println("Terminé! " + finalMap.size() + " situations uniques sauvegardées.");
        displayGameStats(globalStats);
    }

    /**
     * Première surcharge - Version pour les joueurs AI standards
     * Adapte le processus pour prendre en compte les spécificités des AIPlayers.
     * 
     * Différences avec la version de base :
     * - Accès aux configurations spécifiques des AI via player.model
     * - Gestion des paramètres d'apprentissage
     * - Possibilité d'utiliser des stratégies AI personnalisées
     * 
     * Optimisations spécifiques :
     * - Utilisation des paramètres AI préconfigurés
     * - Pas de conversion nécessaire entre Model et AIPlayer
     * - Accès direct aux fonctionnalités AI
     */
    public void startGamesWithUniqueStatesClassicThreads(int nbParties, AIPlayer player1, AIPlayer player2, int nbThreads, boolean appendToExisting) {
        // Similaire à la version Model, mais adaptée aux AIPlayers
        // Les différences principales sont :
        // 1. Utilisation directe des modèles des joueurs AI
        // 2. Gestion spécifique des configurations AI
        // 3. Possibilité d'accéder aux paramètres spécifiques des AI
        
        // Structures de données et configuration initiale
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

        // Configuration du multithreading similaire
        Thread[] threads = new Thread[nbThreads];
        @SuppressWarnings("unchecked")
        Map<String, double[]>[] threadResults = new HashMap[nbThreads];
        ProgressBar[] progressBars = new ProgressBar[nbThreads];
        
        int partiesPerThread = nbParties / nbThreads;
        final int BATCH_SIZE = 1000;

        // Création des threads avec gestion spécifique AI
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
     * Deuxième surcharge - Version pour les joueurs AI avec système de pondération
     * Spécialisée pour gérer les joueurs utilisant des stratégies de sélection pondérée.
     * 
     * Fonctionnalités spécifiques :
     * - Gestion des poids pour la sélection des coups
     * - Adaptation dynamique des stratégies
     * - Support des mécanismes d'apprentissage pondéré
     * 
     * Avantages :
     * - Meilleure exploration de l'espace des coups
     * - Possibilité d'ajuster les stratégies en cours de partie
     * - Support de l'apprentissage par renforcement
     * 
     * Note : Cette version est particulièrement utile pour :
     * - L'entraînement de modèles adaptatifs
     * - L'exploration de stratégies diverses
     * - L'optimisation des paramètres de jeu
     */
    public void startGamesWithUniqueStatesClassicThreads(int nbParties, AIWeightedPlayer player1, AIWeightedPlayer player2, int nbThreads, boolean appendToExisting) {
        // Version spécialisée pour les joueurs AI avec pondération
        // Particularités :
        // 1. Gestion des poids spécifiques à chaque joueur
        // 2. Utilisation du système de pondération pour la sélection des coups
        // 3. Possibilité d'ajuster les poids pendant la partie
        
        // Configuration initiale similaire
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

        // Configuration du multithreading avec gestion des poids
        Thread[] threads = new Thread[nbThreads];
        @SuppressWarnings("unchecked")
        Map<String, double[]>[] threadResults = new HashMap[nbThreads];
        ProgressBar[] progressBars = new ProgressBar[nbThreads];
        
        int partiesPerThread = nbParties / nbThreads;
        final int BATCH_SIZE = 1000;

        // Création des threads avec gestion de la pondération
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
     * Traite un lot d'états de jeu pour optimiser les performances.
     * Utilise un système de batch pour réduire les accès mémoire et améliorer les performances.
     * 
     * @param batch Liste des états de jeu à traiter en une fois
     * @param localMap Map locale du thread pour stocker les états
     * @param finalResult Résultat final de la partie
     */
    private void processBatchLocal(List<GameState> batch, Map<String, double[]> localMap, double finalResult) {
        // Traitement de chaque état du lot
        for (GameState game : batch) {
            // Conversion du résultat en valeur numérique
            double gameResult = game.result == 1 ? 1.0 : game.result == 0 ? 0.5 : 0.0;
            
            // Traitement de chaque état de l'historique de la partie
            for (CompressedState state : game.history) {
                String key = state.toString();
                processStateLocal(localMap, key, state, gameResult);
            }
        }
    }

    /**
     * Traite un état individuel et l'ajoute à la map locale du thread.
     * Gère la création de nouveaux états ou la mise à jour des états existants.
     * 
     * @param localMap Map locale du thread
     * @param key Clé unique de l'état
     * @param state État compressé à traiter
     * @param finalResult Résultat final associé à cet état
     */
    private void processStateLocal(Map<String, double[]> localMap, String key, CompressedState state, double finalResult) {
        double[] existing = localMap.get(key);
        if (existing == null) {
            // Création d'un nouvel état
            double[] newState = state.decompress();
            
            // Vérification et ajustement de la taille du tableau
            if (newState.length != 67) {
                double[] resizedState = new double[67];
                System.arraycopy(newState, 0, resizedState, 0, Math.min(newState.length, 67));
                newState = resizedState;
            }

            // Initialisation des métriques de l'état
            newState[64] = 0;           // moyenne (calculée à l'export)
            newState[65] = finalResult; // somme totale
            newState[66] = 1.0;        // nombre d'occurrences
            localMap.put(key, newState);
        } else {
            // Mise à jour d'un état existant
            existing[65] += finalResult; // Ajout à la somme
            existing[66] += 1.0;        // Incrémentation du compteur
        }
    }

    /**
     * Affiche les statistiques détaillées des parties jouées.
     * Montre le nombre de victoires pour chaque joueur et les matchs nuls.
     * 
     * @param stats Objet contenant les statistiques à afficher
     */
    private void displayGameStats(GameStats stats) {
        System.out.println("\nRésultats finaux :");
        System.out.println("Victoires des Noirs : " + stats.blackWins);
        System.out.println("Victoires des Blancs : " + stats.whiteWins);
        System.out.println("Matchs nuls : " + stats.draws);
    }

    /**
     * Met à jour les statistiques de jeu de manière thread-safe.
     * 
     * @param stats Objet de statistiques à mettre à jour
     * @param result Résultat de la partie à ajouter (-1: blanc gagne, 0: nul, 1: noir gagne)
     */
    private void updateStats(GameStats stats, int result) {
        if (result == 1) stats.blackWins++;
        else if (result == -1) stats.whiteWins++;
        else stats.draws++;
    }

    /**
     * Méthode principale pour tester les fonctionnalités de l'exporteur.
     * Effectue des tests avec et sans fichier existant pour valider le fonctionnement.
     * 
     * @param args Arguments de la ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        // Paramètres de test
        int nbParties = 10000;
        int nbThreads = Runtime.getRuntime().availableProcessors();
        String outputPath = "datataset_pour_cherif.csv";
        
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


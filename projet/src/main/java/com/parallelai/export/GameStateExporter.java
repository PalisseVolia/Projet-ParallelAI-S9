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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.parallelai.exec.play.GameManager;
import com.parallelai.export.implementations.ClassicThreadExporter;
import com.parallelai.export.implementations.OptimizedExporter;
import com.parallelai.export.implementations.ParallelExporter;
import com.parallelai.export.utilities.GameExporterUtils.CompressedState;
import com.parallelai.export.utilities.GameExporterUtils.ProgressBar;
import com.parallelai.export.utilities.GameExporterUtils.StateBuffer;
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
@SuppressWarnings("unused")
public abstract class GameStateExporter {
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
        
        System.out.println("Terminé! " + nbParties + "ont été jouées.");
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
            for (int row = 0; i < 8; row++) {
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
    public int calculateGameResult(Board finalBoard) {
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
        double[] state = new double[67]; // 64 cases + moyenne + somme + occurrences
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

    protected void exportStateMap(Map<String, double[]> stateMap) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            for (double[] state : stateMap.values()) {
                StringBuilder line = new StringBuilder();
                
                // État du plateau (0-63)
                for (int i = 0; i < 64; i++) {
                    line.append(state[i]).append(",");
                }
                
                // Moyenne (64)
                line.append(state[65] / state[66]).append(",");
                // Somme totale (65)
                line.append(state[65]).append(",");
                // Nombre d'occurrences (66)
                line.append(state[66]).append("\n");
                
                writer.write(line.toString());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
        }
    }

    protected Map<String, double[]> loadExistingCSV() {
        Map<String, double[]> existingData = new HashMap<>();
        File file = new File(outputPath);
        
        if (!file.exists()) {
            return existingData;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 67) { // 64 cases + moyenne + somme + count
                    String state = parts[0]; // On utilise la première case comme clé
                    double[] values = new double[67];
                    // Copier l'état du plateau (0-63)
                    for (int i = 0; i < 64; i++) {
                        values[i] = Double.parseDouble(parts[i]);
                    }
                    // La moyenne (64) est recalculée automatiquement
                    values[64] = 0; // sera calculée à l'export
                    // Somme totale (65)
                    values[65] = Double.parseDouble(parts[65]);
                    // Nombre d'occurrences (66)
                    values[66] = Double.parseDouble(parts[66]);
                    existingData.put(state, values);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier CSV: " + e.getMessage());
        }
        return existingData;
    }

    public void streamMerge(List<Map<String, double[]>> threadResults, final Map<String, double[]> finalMap) {
        
        // Parcourir les résultats thread par thread
        for (int i = 0; i < threadResults.size(); i++) {
            Map<String, double[]> currentResult = threadResults.get(i);
            
            // Pour le premier thread, copier directement les résultats
            if (i == 0) {
                currentResult.forEach((key, value) -> finalMap.put(key, value));
            } else {
                // Pour les autres threads, fusionner sans créer de nouveaux objets
                currentResult.forEach((key, value) -> {
                    double[] existing = finalMap.get(key);
                    if (existing != null) {
                        existing[65] += value[65]; // Somme totale
                        existing[66] += value[66]; // Nombre d'occurrences
                        // existing[64] sera recalculé à l'export
                    } else {
                        finalMap.put(key, value);
                    }
                });
            }
            
            // Libérer la mémoire immédiatement
            threadResults.set(i, null);
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


    public void startGamesParallel(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads...\n");
        ProgressBar.initDisplay(nbThreads);
        
        Thread[] threads = new Thread[nbThreads];
        int partiesPerThread = nbParties / nbThreads;
        
        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;
            
            ProgressBar progressBar = new ProgressBar(partiesForThisThread, threadId);
            
            threads[i] = new Thread(() -> {
                int gamesCompleted = 0;
                
                for (int j = 0; j < partiesForThisThread; j++) {
                    Board board = new Board();
                    GameManager game = new GameManager(board, model1, model2);
                    game.startGame();
                    
                    gamesCompleted++;
                    if (gamesCompleted % 10 == 0) {
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
        System.out.println("Terminé! " + nbParties + " parties ont été sauvegardées dans " + outputPath);
    }

    public void startGamesWithProgress(int nbParties, Model model1, Model model2) {
        System.out.println("Début des " + nbParties + " parties...\n");
        ProgressBar progressBar = new ProgressBar(nbParties, 0);
        
        Map<String, double[]> stateMap = new HashMap<>();
        StateBuffer stateBuffer = new StateBuffer();
        
        for (int i = 0; i<nbParties; i++) {
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
            
            if ((i + 1) % 10 == 0) {
                progressBar.update(i + 1);
            }
        }
        
        progressBar.update(nbParties);
        exportStateMap(stateMap);
        System.out.println("\nTerminé! " + stateMap.size() + " situations uniques sauvegardées dans " + outputPath);
    }

    public void startGamesParallelWithProgress(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads...\n");
        ProgressBar.initDisplay(nbThreads);
        
        List<Map<String, double[]>> threadResults = new ArrayList<>(nbThreads);
        Thread[] threads = new Thread[nbThreads];
        int partiesPerThread = nbParties / nbThreads;
        
        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;
            
            Map<String, double[]> threadMap = new HashMap<>();
            threadResults.add(threadMap);
            ProgressBar progressBar = new ProgressBar(partiesForThisThread, threadId);
            
            threads[i] = new Thread(() -> {
                StateBuffer stateBuffer = new StateBuffer();
                
                for (int j = 0; j < partiesForThisThread; j++) {
                    Board board = new Board();
                    GameManager game = new GameManager(board, model1, model2);
                    List<CompressedState> history = new ArrayList<>();
                    
                    while (game.playNextMove()) {
                        history.add(stateBuffer.compressState(board));
                    }
                    
                    double finalResult;
                    int result = calculateGameResult(board);
                    finalResult = result == 1 ? 1.0 : result == 0 ? 0.5 : 0.0;
                    
                    for (CompressedState state : history) {
                        String key = state.toString();
                        threadMap.compute(key, (k, v) -> {
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
                    
                    if ((j + 1) % 10 == 0) {
                        progressBar.update(j + 1);
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
        
        // Fusion des résultats de tous les threads
        Map<String, double[]> finalMap = new HashMap<>();
        streamMerge(threadResults, finalMap);
        
        // Export des résultats
        exportStateMap(finalMap);
        
        System.out.print(String.format("\033[%dH\n", nbThreads + 2));
        System.out.println("Terminé! " + finalMap.size() + " situations uniques sauvegardées dans " + outputPath);
    }

    public static void main(String[] args) {
        String outputPath = "projet\\src\\main\\ressources\\data\\game_history.csv";
        GameStateExporter baseExporter = new ClassicThreadExporter(outputPath);
        ParallelExporter parallelExporter = new ParallelExporter(outputPath);
        ClassicThreadExporter classicExporter = new ClassicThreadExporter(outputPath);
        OptimizedExporter optimizedExporter = new OptimizedExporter(outputPath);
        
        Model model1 = new RandomModel();
        Model model2 = new RandomModel();
         
        int nbParties = 20000;
        int nbThreads = Runtime.getRuntime().availableProcessors();

        System.out.println("Début du test avec " + nbParties + " parties sur " + nbThreads + " threads...");
        
        // Warm-up
        System.out.println("Warm-up...");
        baseExporter.startGamesWithUniqueStatesSequential(1000, model1, model2);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n=== Début des tests de performance ===");

        // Test séquentiel avec progression
        System.out.println("\n=== Test séquentiel avec progression ===");
        long startTimeSeqProg = System.currentTimeMillis();
        baseExporter.startGamesWithProgress(nbParties, model1, model2);
        long endTimeSeqProg = System.currentTimeMillis();
        double executionTimeSeqProg = (endTimeSeqProg - startTimeSeqProg) / 1000.0;

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Test parallèle avec progression
        System.out.println("\n=== Test parallèle avec progression ===");
        long startTimeParProg = System.currentTimeMillis();
        baseExporter.startGamesParallelWithProgress(nbParties, model1, model2, nbThreads);
        long endTimeParProg = System.currentTimeMillis();
        double executionTimeParProg = (endTimeParProg - startTimeParProg) / 1000.0;

        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Tests parallèles
        System.out.println("\n=== Tests parallèles (" + nbThreads + " threads) ===");
        
        // Test avec ConcurrentHashMap
        long startTimePar = System.currentTimeMillis();
        parallelExporter.startGamesWithUniqueStatesParallel(nbParties, model1, model2, nbThreads);
        long endTimePar = System.currentTimeMillis();
        double executionTimePar = (endTimePar - startTimePar) / 1000.0;

        // Test sans synchronisation
        long startTimePar2 = System.currentTimeMillis();
        parallelExporter.startGamesWithUniqueStatesParallelNoSync(nbParties, model1, model2, nbThreads);
        long endTimePar2 = System.currentTimeMillis();
        double executionTimePar2 = (endTimePar2 - startTimePar2) / 1000.0;
        
        // Test avec threads classiques
        long startTimeClassic = System.currentTimeMillis();
        classicExporter.startGamesWithUniqueStatesClassicThreads(nbParties, model1, model2, nbThreads,false);
        long endTimeClassic = System.currentTimeMillis();
        double executionTimeClassic = (endTimeClassic - startTimeClassic) / 1000.0;
        
   
        // Affichage des résultats
        System.out.println("\n=== Comparaison des performances ===");
        System.out.printf("Nombre de parties: %d\n", nbParties);
        System.out.printf("Nombre de threads: %d\n", nbThreads);

        System.out.printf("Version Séquentielle + Progress  : %.2f secondes\n", 
            executionTimeSeqProg);
        System.out.printf("Version Parallèle + Progress     : %.2f secondes (x%.2f)\n", 
            executionTimeParProg, executionTimeSeqProg/executionTimeParProg);
        System.out.printf("Version HashMap                  : %.2f secondes (x%.2f)\n", 
            executionTimePar2, executionTimeSeqProg/executionTimePar2);
        System.out.printf("Version ConcurrentMap            : %.2f secondes (x%.2f)\n", 
            executionTimePar, executionTimeSeqProg/executionTimePar);
        System.out.printf("Version Thread classique         : %.2f secondes (x%.2f)\n", 
            executionTimeClassic, executionTimeSeqProg/executionTimeClassic);
      
    }
}
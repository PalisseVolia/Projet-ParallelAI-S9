package com.parallelai.export;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    public void startGamesWithUniqueStatesParallel(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads...");
        
        // Créer les threads et leurs résultats associés
        List<GameThread> threads = new ArrayList<>();
        int partiesPerThread = nbParties / nbThreads;
        
        // Lancer les threads
        for (int i = 0; i < nbThreads; i++) {
            int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;
                
            GameThread thread = new GameThread(partiesForThisThread, model1, model2);
            threads.add(thread);
            thread.start();
        }
        
        // Attendre la fin de tous les threads
        List<List<double[]>> threadResults = new ArrayList<>();
        for (GameThread thread : threads) {
            try {
                thread.join();
                threadResults.add(thread.getUniqueStates());
            } catch (InterruptedException e) {
                System.err.println("Thread interrompu: " + e.getMessage());
            }
        }
        
        // Fusionner les résultats
        List<double[]> mergedStates = mergeThreadResults(threadResults);
        
        // Exporter le résultat final
        exportUniqueStatesArray(mergedStates);
        System.out.println("Terminé! " + mergedStates.size() + " situations uniques sauvegardées.");
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
    
    private class GameThread extends Thread {
        private final int nbParties;
        private final Model model1;
        private final Model model2;
        private List<double[]> uniqueStates;
        
        public GameThread(int nbParties, Model model1, Model model2) {
            this.nbParties = nbParties;
            this.model1 = model1;
            this.model2 = model2;
            this.uniqueStates = new ArrayList<>();
        }
        
        @Override
        public void run() {
            for (int i = 0; i < nbParties; i++) {
                Board board = new Board();
                GameManager game = new GameManager(board, model1, model2);
                List<Board> gameHistory = new ArrayList<>();
                
                while (game.playNextMove()) {
                    gameHistory.add(board.copy());
                }
                
                // Calculer le résultat final
                double finalResult;
                int gameResult = calculateGameResult(board);
                if (gameResult == 1) {
                    finalResult = 1.0;      // Victoire noire
                } else if (gameResult == -1) {
                    finalResult = 0.0;      // Défaite noire
                } else {
                    finalResult = 0.5;      // Match nul
                }
                
                // Traiter chaque état du jeu
                for (Board state : gameHistory) {
                    double[] currentState = boardToArray(state);
                    boolean found = false;
                    
                    // Chercher si la situation existe déjà
                    for (double[] existingState : uniqueStates) {
                        if (isSameState(currentState, existingState)) {
                            existingState[64] += finalResult;  // Somme des résultats
                            existingState[65] += 1.0;         // Nombre d'occurrences
                            found = true;
                            break;
                        }
                    }
                    
                    if (!found) {
                        currentState[64] = finalResult;  // Premier résultat
                        currentState[65] = 1.0;         // Première occurrence
                        uniqueStates.add(currentState);
                    }
                }

                if ((i + 1) % 10 == 0) {
                    System.out.println("Thread " + Thread.currentThread().getId() + 
                                     ": " + (i + 1) + "/" + nbParties + " parties terminées");
                }
            }
        }
        
        public List<double[]> getUniqueStates() {
            return uniqueStates;
        }
    }

    // Modifier le main pour tester la version parallèle
    public static void main(String[] args) {
        // Création de l'exporteur
        GameStateExporter exporter = new GameStateExporter("projet\\src\\main\\ressources\\data\\game_history.csv");
        
        // Création des modèles pour le test
        Model model1 = new MinimaxModel(); // Un modèle plus intelligent
        Model model2 = new RandomModel();   // Un modèle aléatoire
        
        // Configuration du test
        int nbParties = 100;  // Nombre de parties à jouer
        int nbThreads = Runtime.getRuntime().availableProcessors(); // Utiliser tous les cœurs disponibles
        
        System.out.println("Début du test avec " + nbParties + " parties sur " + nbThreads + " threads...");
        
        long startTime = System.currentTimeMillis();
        exporter.startGamesWithUniqueStatesParallel(nbParties, model1, model2, nbThreads);
        long endTime = System.currentTimeMillis();
        
        double executionTime = (endTime - startTime) / 1000.0;
        System.out.println("Test terminé en " + executionTime + " secondes");
    }
}

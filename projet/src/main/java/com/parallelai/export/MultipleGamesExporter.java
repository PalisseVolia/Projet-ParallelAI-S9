// package com.parallelai.export;

// import java.io.FileWriter;
// import java.io.IOException;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.Objects;

// import com.parallelai.game.Board;
// import com.parallelai.game.Disc;
// import com.parallelai.game.Move;
// import com.parallelai.game.Player;
// import com.parallelai.players.RandomAIPlayer;

// /**
//  * Gestionnaire d'export de données pour l'apprentissage automatique d'Othello.
//  * Cette classe permet de:
//  * - Jouer plusieurs parties entre IAs aléatoires
//  * - Sauvegarder chaque état de jeu au format CSV
//  * - Générer un dataset d'entraînement pour le machine learning
//  */
// public class MultipleGamesExporter {
//     /** Chemin du fichier CSV de sortie */
//     private final String outputPath;
    
//     /** Nombre total de parties à jouer */
//     private final int nbParties;
    
//     /** Gestionnaire d'export pour une situation donnée */
//     private final GameStateExporter exporter;

//     /**
//      * Crée un gestionnaire d'export de parties multiples.
//      * @param outputPath Chemin du fichier CSV où sauvegarder les données
//      * @param nbParties Nombre de parties à jouer et exporter
//      */
//     public MultipleGamesExporter(String outputPath, int nbParties) {
//         this.outputPath = outputPath;
//         this.nbParties = nbParties;
//         this.exporter = new GameStateExporter(outputPath);
//     }

//     /**
//      * Lance l'export des parties.
//      * Pour chaque partie:
//      * - Fait jouer deux IA aléatoires
//      * - Sauvegarde chaque état du plateau dans le CSV
//      * - Ajoute pour chaque état si la position est gagnante
//      * Affiche la progression de l'export.
//      */
//     public void playAndExportGames() {
//         System.out.println("Début de l'export de " + nbParties + " parties...");
        
//         for (int i = 0; i < nbParties; i++) {
//             // Initialiser une nouvelle partie
//             Board board = new Board();
//             RandomAIPlayer player1 = new RandomAIPlayer(Disc.BLACK);
//             RandomAIPlayer player2 = new RandomAIPlayer(Disc.WHITE);
//             Player currentPlayer = player1;
            
//             // Jouer jusqu'à la fin
//             while (board.hasValidMoves(Disc.BLACK) || board.hasValidMoves(Disc.WHITE)) {
//                 if (board.hasValidMoves(currentPlayer.getColor())) {
//                     Move move = currentPlayer.getMove(board);
//                     if (move != null) {
//                         board.makeMove(move);
                        
//                         // Exporter la situation du point de vue du joueur courant
//                         boolean isWinning = isWinningPosition(board, currentPlayer.getColor());
//                         exporter.exportState(board, isWinning);
//                     }
//                 }
//                 // Changer de joueur
//                 currentPlayer = (currentPlayer == player1) ? player2 : player1;
//             }
            
//             if ((i + 1) % 10 == 0) {
//                 System.out.println("Parties exportées: " + (i + 1) + "/" + nbParties);
//             }
//         }
//         System.out.println("Export terminé!");
//     }

//     /**
//      * Détermine si une position est gagnante pour un joueur.
//      * Compare le nombre de pions de chaque couleur.
//      * @param board État du plateau à évaluer
//      * @param playerColor Couleur du joueur à évaluer
//      * @return true si la position est gagnante pour le joueur
//      */
//     private boolean isWinningPosition(Board board, Disc playerColor) {
//         int blackCount = board.getDiscCount(Disc.BLACK);
//         int whiteCount = board.getDiscCount(Disc.WHITE);
        
//         if (playerColor == Disc.BLACK) {
//             return blackCount > whiteCount;
//         } else {
//             return whiteCount > blackCount;
//         }
//     }

//     /**
//      * Représente une situation de jeu unique
//      */
//     private static class BoardSituation {
//         private final String boardState;
//         private int winCount;
//         private int totalCount;

//         public BoardSituation(String boardState) {
//             this.boardState = boardState;
//             this.winCount = 0;
//             this.totalCount = 0;
//         }

//         public void addOccurrence(boolean isWinning) {
//             if (isWinning) winCount++;
//             totalCount++;
//         }

//         @Override
//         public boolean equals(Object o) {
//             if (this == o) return true;
//             if (o == null || getClass() != o.getClass()) return false;
//             BoardSituation that = (BoardSituation) o;
//             return Objects.equals(boardState, that.boardState);
//         }

//         @Override
//         public int hashCode() {
//             return Objects.hash(boardState);
//         }
//     }

//     /**
//      * Crée un dataset sans doublons en comptant les occurrences de chaque situation.
//      */
//     public void playAndExportUniqueGames() {
//         System.out.println("Début de l'export de " + nbParties + " parties avec dédoublonnage...");
//         Map<String, BoardSituation> situations = new HashMap<>();
        
//         // Jouer les parties et collecter les situations
//         for (int i = 0; i < nbParties; i++) {
//             Board board = new Board();
//             RandomAIPlayer player1 = new RandomAIPlayer(Disc.BLACK);
//             RandomAIPlayer player2 = new RandomAIPlayer(Disc.WHITE);
//             Player currentPlayer = player1;
            
//             while (board.hasValidMoves(Disc.BLACK) || board.hasValidMoves(Disc.WHITE)) {
//                 if (board.hasValidMoves(currentPlayer.getColor())) {
//                     Move move = currentPlayer.getMove(board);
//                     if (move != null) {
//                         board.makeMove(move);
                        
//                         boolean isWinning = isWinningPosition(board, currentPlayer.getColor());
//                         String boardState = boardToString(board);
                        
//                         situations.computeIfAbsent(boardState, BoardSituation::new)
//                                 .addOccurrence(isWinning);
//                     }
//                 }
//                 currentPlayer = (currentPlayer == player1) ? player2 : player1;
//             }
            
//             if ((i + 1) % 10 == 0) {
//                 System.out.println("Parties analysées: " + (i + 1) + "/" + nbParties);
//             }
//         }
        
//         // Écrire les situations uniques dans un nouveau fichier
//         String uniqueOutputPath = outputPath.replace(".csv", "_unique.csv");
//         try (FileWriter writer = new FileWriter(uniqueOutputPath)) {
//             for (BoardSituation situation : situations.values()) {
//                 writer.write(situation.boardState);
//                 writer.write(",");
//                 writer.write(String.valueOf((double) situation.winCount / situation.totalCount));
//                 writer.write(",");
//                 writer.write(String.valueOf(situation.totalCount));
//                 writer.write("\n");
//             }
//         } catch (IOException e) {
//             System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
//         }

//         System.out.println("Export terminé! " + situations.size() + " situations uniques trouvées.");
//     }

//     /**
//      * Convertit un plateau en chaîne de caractères CSV
//      */
//     private String boardToString(Board board) {
//         StringBuilder sb = new StringBuilder();
//         Disc[][] grid = board.getGrid();
        
//         for (int i = 0; i < 8; i++) {
//             for (int j = 0; j < 8; j++) {
//                 if (grid[i][j] == Disc.BLACK) sb.append("1");
//                 else if (grid[i][j] == Disc.WHITE) sb.append("-1");
//                 else sb.append("0");
//                 sb.append(",");
//             }
//         }
//         return sb.substring(0, sb.length() - 1); // Enlever la dernière virgule
//     }

//     /**
//      * Point d'entrée pour lancer un export.
//      * @param args Arguments non utilisés
//      */
//     public static void main(String[] args) {
//         int nbParties = 100; // ou autre nombre souhaité
//         MultipleGamesExporter exporter = new MultipleGamesExporter("training_data.csv", nbParties);
//         exporter.playAndExportUniqueGames(); // Utilise la nouvelle fonction pour éviter les doublons
//     }
// }

package com.parallelai;

public class MultipleGamesExporter {
    private final String outputPath;
    private final int nbParties;
    private final GameStateExporter exporter;

    public MultipleGamesExporter(String outputPath, int nbParties) {
        this.outputPath = outputPath;
        this.nbParties = nbParties;
        this.exporter = new GameStateExporter(outputPath);
    }

    public void playAndExportGames() {
        System.out.println("Début de l'export de " + nbParties + " parties...");
        
        for (int i = 0; i < nbParties; i++) {
            // Initialiser une nouvelle partie
            Board board = new Board();
            RandomAIPlayer player1 = new RandomAIPlayer(Disc.BLACK);
            RandomAIPlayer player2 = new RandomAIPlayer(Disc.WHITE);
            Player currentPlayer = player1;
            
            // Jouer jusqu'à la fin
            while (board.hasValidMoves(Disc.BLACK) || board.hasValidMoves(Disc.WHITE)) {
                if (board.hasValidMoves(currentPlayer.getColor())) {
                    Move move = currentPlayer.getMove(board);
                    if (move != null) {
                        board.makeMove(move);
                        
                        // Exporter la situation du point de vue du joueur courant
                        boolean isWinning = isWinningPosition(board, currentPlayer.getColor());
                        exporter.exportState(board, isWinning);
                    }
                }
                // Changer de joueur
                currentPlayer = (currentPlayer == player1) ? player2 : player1;
            }
            
            if ((i + 1) % 10 == 0) {
                System.out.println("Parties exportées: " + (i + 1) + "/" + nbParties);
            }
        }
        System.out.println("Export terminé!");
    }
    
    private boolean isWinningPosition(Board board, Disc playerColor) {
        int blackCount = board.getDiscCount(Disc.BLACK);
        int whiteCount = board.getDiscCount(Disc.WHITE);
        
        if (playerColor == Disc.BLACK) {
            return blackCount > whiteCount;
        } else {
            return whiteCount > blackCount;
        }
    }

    public static void main(String[] args) {
        int nbParties = 100; // ou autre nombre souhaité
        MultipleGamesExporter exporter = new MultipleGamesExporter("training_data.csv", nbParties);
        exporter.playAndExportGames();
    }
}

package com.parallelai;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GameManager {
    private List<BoardState> gameHistory;
    private Scanner scanner;
    private Board board;
    private Player player1;
    private Player player2;
    private Player currentPlayer; // Ajout de la variable d'instance
    private boolean isGameOver;
    private GameStateExporter exporter;

    public GameManager() {
        this.gameHistory = new ArrayList<>();
        this.scanner = new Scanner(System.in);
        this.board = new Board();
        initializePlayers();
        this.currentPlayer = player1; // Initialisation
        this.exporter = new GameStateExporter("game_states.csv");
    }

    public GameManager(Board board, RandomAIPlayer player1, RandomAIPlayer player2) {
        this.gameHistory = new ArrayList<>();
        this.scanner = new Scanner(System.in);
        this.board = board;
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1; // Initialisation
        this.isGameOver = false;
    }
    
    private void initializePlayers() {
        System.out.println("Choose game mode:");
        System.out.println("1. Human vs Human");
        System.out.println("2. Human vs AI");
        System.out.println("3. AI vs AI");
        
        int choice = scanner.nextInt();
        switch (choice) {
            case 1:
                player1 = new HumanPlayer(Disc.BLACK, scanner);
                player2 = new HumanPlayer(Disc.WHITE, scanner);
                break;
            case 2:
                player1 = new HumanPlayer(Disc.BLACK, scanner);
                player2 = new AIPlayer(Disc.WHITE);
                break;
            case 3:
                player1 = new AIPlayer(Disc.BLACK);
                player2 = new AIPlayer(Disc.WHITE);
                break;
        }
    }

    public void startGame() {
        while (!isGameOver) {
            board.display();
            saveGameState();
            
            if (board.hasValidMoves(currentPlayer.getColor())) {
                System.out.println("Current player: " + currentPlayer.getColor());
                Move move = currentPlayer.getMove(board);
                if (move != null) {
                    board.makeMove(move);
                }
            } else {
                System.out.println("No valid moves for " + currentPlayer.getColor());
                if (!board.hasValidMoves(currentPlayer.getColor().opposite())) {
                    isGameOver = true;
                    break;
                }
            }
            
            // Mise à jour du joueur courant
            currentPlayer = (currentPlayer == player1) ? player2 : player1;
        }
        
        announceWinner();
    }

    private void saveGameState() {
        // Déterminer si le joueur actuel a gagné
        int blackCount = board.getDiscCount(Disc.BLACK);
        int whiteCount = board.getDiscCount(Disc.WHITE);
        boolean blackWon = blackCount > whiteCount;
        
        // Sauvegarder l'état
        if (currentPlayer.getColor() == Disc.BLACK) {
            exporter.exportState(board, blackWon);
        } else {
            exporter.exportState(board, !blackWon);
        }
    }

    private void announceWinner() {
        board.display();
        int blackCount = board.getDiscCount(Disc.BLACK);
        int whiteCount = board.getDiscCount(Disc.WHITE);
        
        System.out.println("Game Over!");
        System.out.println("Black: " + blackCount);
        System.out.println("White: " + whiteCount);
        
        if (blackCount > whiteCount) {
            System.out.println("Black wins!");
        } else if (whiteCount > blackCount) {
            System.out.println("White wins!");
        } else {
            System.out.println("It's a tie!");
        }
    }

    public static void playRandomAIGame() {
        Board board = new Board();
        RandomAIPlayer player1 = new RandomAIPlayer(Disc.BLACK);
        RandomAIPlayer player2 = new RandomAIPlayer(Disc.WHITE);
        
        GameManager game = new GameManager(board, player1, player2);
        game.startGame();
    }

    public static void main(String[] args) {
        //GameManager game = new GameManager();
        //game.startGame();
        playRandomAIGame();
    }
}

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
    private boolean isGameOver;

    public GameManager() {
        this.gameHistory = new ArrayList<>();
        this.scanner = new Scanner(System.in);
        this.board = new Board();
        initializePlayers();
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
        Player currentPlayer = player1;
        
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
            
            currentPlayer = (currentPlayer == player1) ? player2 : player1;
        }
        
        announceWinner();
    }

    private void saveGameState() {
        gameHistory.add(new BoardState(board));
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

    public static void main(String[] args) {
        GameManager game = new GameManager();
        game.startGame();
    }
}

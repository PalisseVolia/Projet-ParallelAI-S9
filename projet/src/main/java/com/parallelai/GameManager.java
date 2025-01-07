package com.parallelai;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

import com.parallelai.export.GameStateExporter;
import com.parallelai.game.*;
import com.parallelai.players.*;
import com.parallelai.models.utils.*;

public class GameManager {
    // Game mode enums for cleaner code
    private enum GameMode { HUMAN_VS_HUMAN, HUMAN_VS_AI, AI_VS_AI }
    private enum AIType { REGULAR, WEIGHTED }

    private final Scanner scanner;
    private final Board board;
    private Player player1;
    private Player player2;
    private Player currentPlayer;
    private final List<Board> gameHistory;
    private boolean isGameOver;

    // Game statistics
    private String model1Name;
    private String model2Name;
    private int model1Wins;
    private int model2Wins;
    private int ties;

    public GameManager() {
        this.scanner = new Scanner(System.in);
        this.board = new Board();
        this.gameHistory = new ArrayList<>();
        this.isGameOver = false;
    }

    public GameManager(Board board, Model model1, Model model2) {
        this.scanner = new Scanner(System.in);
        this.board = board;
        this.player1 = new UnifiedAIPlayer(Disc.BLACK, model1);
        this.player2 = new UnifiedAIPlayer(Disc.WHITE, model2);
        this.currentPlayer = player1;
        this.gameHistory = new ArrayList<>();
        this.isGameOver = false;
    }

    /**
     * Initializes and starts the game based on user input
     */
    public void initialize() {
        GameMode gameMode = promptGameMode();
        setupPlayers(gameMode);
        
        if (gameMode == GameMode.AI_VS_AI) {
            handleAIGame();
        } else {
            startGame();
        }
    }

    /**
     * Prompts for game mode selection
     */
    private GameMode promptGameMode() {
        System.out.println("Choose game mode:");
        System.out.println("1. Human vs Human");
        System.out.println("2. Human vs AI");
        System.out.println("3. AI vs AI");
        
        int choice = scanner.nextInt();
        return switch (choice) {
            case 1 -> GameMode.HUMAN_VS_HUMAN;
            case 2 -> GameMode.HUMAN_VS_AI;
            case 3 -> GameMode.AI_VS_AI;
            default -> throw new IllegalArgumentException("Invalid game mode");
        };
    }

    /**
     * Sets up players based on the selected game mode
     */
    private void setupPlayers(GameMode gameMode) {
        switch (gameMode) {
            case HUMAN_VS_HUMAN:
                player1 = new HumanPlayer(Disc.BLACK, scanner);
                player2 = new HumanPlayer(Disc.WHITE, scanner);
                break;
            case HUMAN_VS_AI:
                player1 = new HumanPlayer(Disc.BLACK, scanner);
                Model aiModel = selectAIModel("Select AI model for White");
                player2 = new UnifiedAIPlayer(Disc.WHITE, aiModel);
                break;
            case AI_VS_AI:
                // Players will be set up in handleAIGame()
                break;
        }
        currentPlayer = player1;
    }

    /**
     * Handles AI vs AI game setup and execution
     */
    private void handleAIGame() {
        AIType aiType = promptAIType();
        System.out.println("Enter number of games (1 for single game):");
        int numGames = scanner.nextInt();

        Model model1 = selectAIModel("Select AI model for Black");
        Model model2 = selectAIModel("Select AI model for White");
        
        if (numGames == 1) {
            setupAIPlayers(model1, model2, aiType);
            startGame();
        } else {
            runMultipleGames(numGames, model1, model2, aiType);
        }
    }

    /**
     * Prompts for AI type selection
     */
    private AIType promptAIType() {
        System.out.println("Choose AI type:");
        System.out.println("1. Regular AI   - Best moves");
        System.out.println("2. Weighted AI  - Weighted random selection");
        
        return scanner.nextInt() == 1 ? AIType.REGULAR : AIType.WEIGHTED;
    }

    /**
     * Sets up AI players based on selected models and type
     */
    private void setupAIPlayers(Model model1, Model model2, AIType aiType) {
        if (aiType == AIType.REGULAR) {
            player1 = new UnifiedAIPlayer(Disc.BLACK, model1);
            player2 = new UnifiedAIPlayer(Disc.WHITE, model2);
        } else {
            player1 = new UnifiedWeightedAIPlayer(Disc.BLACK, model1);
            player2 = new UnifiedWeightedAIPlayer(Disc.WHITE, model2);
        }
        currentPlayer = player1;
    }

    /**
     * Runs multiple games and displays statistics
     */
    private void runMultipleGames(int numGames, Model model1, Model model2, AIType aiType) {
        model1Name = model1.getName();
        model2Name = model2.getName();
        
        for (int i = 0; i < numGames; i++) {
            board.reset();
            setupAIPlayers(model1, model2, aiType);
            isGameOver = false;
            
            while (playNextMove()) {
                // Game continues
            }
            
            updateGameStatistics();
        }
        
        displayGameStatistics(numGames);
    }

    private void updateGameStatistics() {
        int blackCount = board.getDiscCount(Disc.BLACK);
        int whiteCount = board.getDiscCount(Disc.WHITE);
        
        if (blackCount > whiteCount) model1Wins++;
        else if (whiteCount > blackCount) model2Wins++;
        else ties++;
    }

    private void displayGameStatistics(int numGames) {
        System.out.println("Results after " + numGames + " games:");
        System.out.println(model1Name + " wins: " + model1Wins);
        System.out.println(model2Name + " wins: " + model2Wins);
        System.out.println("Ties: " + ties);
    }

    /**
     * Allows user to select an AI model
     */
    private Model selectAIModel(String prompt) {
        List<ModelRegistry.ModelInfo> models = ModelRegistry.getAvailableModels();
        System.out.println(prompt + ":");
        for (int i = 0; i < models.size(); i++) {
            System.out.println((i + 1) + ". " + models.get(i).name);
        }
        return ModelRegistry.createModel(scanner.nextInt() - 1);
    }

    // Core game methods maintained for compatibility
    public void startGame(boolean save) {
        while (!isGameOver) {
            board.display();
            gameHistory.add(board.copy());
            
            if (!processNextMove()) {
                break;
            }
            currentPlayer = (currentPlayer == player1) ? player2 : player1;
        }
        
        if (save) {
            GameStateExporter exporter = new GameStateExporter("game_history.csv");
            exporter.exportGame(gameHistory, board);
        }
        announceWinner();
    }

    public void startGame() {
        startGame(false);
    }

    private boolean processNextMove() {
        if (!board.hasValidMoves(currentPlayer.getColor())) {
            // System.out.println("No valid moves for " + currentPlayer.getColor());
            if (!board.hasValidMoves(currentPlayer.getColor().opposite())) {
                isGameOver = true;
                return false;
            }
            return true;
        }

        // System.out.println("Current player: " + currentPlayer.getColor());
        Move move = currentPlayer.getMove(board);
        if (move != null) {
            board.makeMove(move);
        }
        return true;
    }

    public boolean playNextMove() {
        if (isGameOver) return false;
        
        if (!processNextMove()) {
            return false;
        }
        
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        return true;
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

    public Board getBoard() {
        return board;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public static void main(String[] args) {
        GameManager game = new GameManager();
        game.initialize();
    }
}

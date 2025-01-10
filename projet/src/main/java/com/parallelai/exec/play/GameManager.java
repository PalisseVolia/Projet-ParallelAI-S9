package com.parallelai.exec.play;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.parallelai.exec.play.GameRunner.AIType;
import com.parallelai.export.GameStateExporter;
import com.parallelai.export.implementations.ClassicThreadExporter;
import com.parallelai.game.*;
import com.parallelai.players.*;
import com.parallelai.models.utils.*;

public class GameManager {
    // Game mode enums for cleaner code
    private enum GameMode { HUMAN_VS_HUMAN, HUMAN_VS_AI, AI_VS_AI }

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

    // Add these fields for thread-safe statistics
    private final AtomicInteger atomicModel1Wins = new AtomicInteger(0);
    private final AtomicInteger atomicModel2Wins = new AtomicInteger(0);
    private final AtomicInteger atomicTies = new AtomicInteger(0);
    private final AtomicInteger gamesCompleted = new AtomicInteger(0);
    private int totalGames;

    public GameManager() {
        this.scanner = new Scanner(System.in);
        this.board = new Board();
        this.gameHistory = new ArrayList<>();
        this.isGameOver = false;
    }

    public GameManager(Board board, Model model1, Model model2) {
        this.scanner = new Scanner(System.in);
        this.board = board;
        this.player1 = new AIPlayer(Disc.BLACK, model1);
        this.player2 = new AIPlayer(Disc.WHITE, model2);
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
                player2 = new AIPlayer(Disc.WHITE, aiModel);
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
        totalGames = numGames;

        Model model1 = selectAIModel("Select AI model for Black");
        Model model2 = selectAIModel("Select AI model for White");
        
        // Create players with the selected models and AI type
        if (aiType == AIType.REGULAR) {
            player1 = new AIPlayer(Disc.BLACK, model1);
            player2 = new AIPlayer(Disc.WHITE, model2);
        } else {
            player1 = new AIWeightedPlayer(Disc.BLACK, model1);
            player2 = new AIWeightedPlayer(Disc.WHITE, model2);
        }
        currentPlayer = player1;
        
        if (numGames == 1) {
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
     * Runs multiple games and displays statistics
     */
    private void runMultipleGames(int numGames, Model model1, Model model2, AIType aiType) {
        model1Name = model1.getName();
        model2Name = model2.getName();
        
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(processors);
        List<Future<GameResult>> futures = new ArrayList<>();
        
        System.out.println("Progress: ");
        
        // Submit all games to the executor
        for (int i = 0; i < numGames; i++) {
            GameRunner runner = new GameRunner(model1, model2, aiType, () -> {
                gamesCompleted.incrementAndGet();
                updateProgressBar();
            });
            futures.add(executor.submit(runner));
        }
        
        // Collect results
        for (Future<GameResult> future : futures) {
            try {
                GameResult result = future.get();
                switch (result) {
                    case BLACK_WINS -> atomicModel1Wins.incrementAndGet();
                    case WHITE_WINS -> atomicModel2Wins.incrementAndGet();
                    case TIE -> atomicTies.incrementAndGet();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println(); // New line after progress bar
        
        // Update the original statistics variables for compatibility
        model1Wins = atomicModel1Wins.get();
        model2Wins = atomicModel2Wins.get();
        ties = atomicTies.get();
        
        displayGameStatistics(numGames);
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
            GameStateExporter exporter = new ClassicThreadExporter("game_history.csv");
            exporter.exportGame(gameHistory, board);
        }
        announceWinner();
    }

    public void startGame() {
        startGame(false);
    }

    private boolean processNextMove() {
        if (!board.hasValidMoves(currentPlayer.getColor())) {
            if (player1 instanceof HumanPlayer || player2 instanceof HumanPlayer) {
                System.out.println("No valid moves for " + currentPlayer.getColor());
            }
            if (!board.hasValidMoves(currentPlayer.getColor().opposite())) {
                isGameOver = true;
                return false;
            }
            return true;
        }
        if (player1 instanceof HumanPlayer || player2 instanceof HumanPlayer) {
            System.out.println("Current player: " + currentPlayer.getColor());
        }
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

    private void updateProgressBar() {
        int completed = gamesCompleted.get();
        int percentage = completed * 100 / totalGames;
        int bars = percentage / 2;
        
        synchronized (System.out) {
            StringBuilder progressBar = new StringBuilder("\r[");
            for (int j = 0; j < 50; j++) {
                progressBar.append(j < bars ? "=" : " ");
            }
            progressBar.append("] ").append(percentage).append("% (")
                      .append(completed).append("/").append(totalGames).append(")");
            System.out.print(progressBar);
        }
    }
}

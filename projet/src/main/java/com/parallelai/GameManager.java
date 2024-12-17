package com.parallelai;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

import com.parallelai.export.GameStateExporter;
import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;
import com.parallelai.players.HumanPlayer;
import com.parallelai.players.UnifiedAIPlayer;
import com.parallelai.players.UnifiedWeightedAIPlayer;
import com.parallelai.models.utils.Model;
import com.parallelai.models.utils.ModelRegistry;

/**
 * Gestionnaire de partie d'Othello.
 * Cette classe gère le déroulement d'une partie d'Othello, avec:
 * - Gestion des joueurs (humains ou IA)
 * - Application des règles du jeu
 */
public class GameManager {
    /** Scanner pour la saisie utilisateur */
    private Scanner scanner;
    
    /** Plateau de jeu */
    private Board board;
    
    /** Premier joueur (pions noirs) */
    private Player player1;
    
    /** Second joueur (pions blancs) */
    private Player player2;
    
    /** Joueur dont c'est le tour */
    private Player currentPlayer;

    /** Historique des états de jeu */
    private List<Board> gameHistory = new ArrayList<>();
    private boolean isGameOver;

    /**
     * Constructeur pour une nouvelle partie interactive.
     * Initialise un nouveau plateau et permet de choisir le mode de jeu.
     */
    public GameManager() {
        this.scanner = new Scanner(System.in);
        this.board = new Board();
        initializePlayers();
        this.currentPlayer = player1; // Initialisation
    }
    
    /**
     * Constructor for a game between two models
     * @param board The game board
     * @param model1 Model for player 1
     * @param model2 Model for player 2
     */
    public GameManager(Board board, Model model1, Model model2) {
        this.scanner = new Scanner(System.in);
        this.board = board;
        this.player1 = new UnifiedAIPlayer(Disc.BLACK, model1);
        this.player2 = new UnifiedAIPlayer(Disc.WHITE, model2);
        this.currentPlayer = player1;
        this.isGameOver = false;
    }

    /**
     * Constructor for a game between two models with weighted selection
     * @param board The game board
     * @param model1 Model for player 1
     * @param model2 Model for player 2
     */
    public GameManager(Board board, Model model1, Model model2, boolean Weighted) {
        this.scanner = new Scanner(System.in);
        this.board = board;
        this.player1 = new UnifiedWeightedAIPlayer(Disc.BLACK, model1);
        this.player2 = new UnifiedWeightedAIPlayer(Disc.WHITE, model2);
        this.currentPlayer = player1;
        this.isGameOver = false;
    }
    
    /**
     * Configure les joueurs selon le mode de jeu choisi par l'utilisateur.
     * Propose 3 modes : Humain vs Humain, Humain vs IA, IA vs IA
     */
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
                player2 = new UnifiedAIPlayer(Disc.WHITE, selectAIModel("Select AI model for White"));
                break;
            case 3:
                player1 = new UnifiedAIPlayer(Disc.BLACK, selectAIModel("Select AI model for Black"));
                player2 = new UnifiedAIPlayer(Disc.WHITE, selectAIModel("Select AI model for White"));
                break;
        }
    }

    /**
     * Permet à l'utilisateur de sélectionner un modèle d'IA parmi les modèles disponibles.
     * @param prompt Message à afficher pour inviter l'utilisateur à faire un choix.
     * @return Le modèle d'IA sélectionné.
     */
    private Model selectAIModel(String prompt) {
        List<ModelRegistry.ModelInfo> models = ModelRegistry.getAvailableModels();
        System.out.println(prompt + ":");
        for (int i = 0; i < models.size(); i++) {
            System.out.println((i + 1) + ". " + models.get(i).name);
        }
        int choice = scanner.nextInt() - 1;
        return ModelRegistry.createModel(choice);
    }

    /**
     * Lance et gère le déroulement de la partie.
     * Alterne les tours entre les joueurs jusqu'à la fin de partie.
     * Affiche le plateau et sauvegarde les états de jeu.
     */
    public void startGame() {
        while (!isGameOver) {
            board.display();
            gameHistory.add(board.copy()); // Ajouter une copie du plateau actuel
            
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

   /**
     * Lance et gère le déroulement de la partie.
     * Alterne les tours entre les joueurs jusqu'à la fin de partie.
     * Affiche le plateau et sauvegarde les états de jeu.
     */
    public void startGame(boolean save) {
        while (!isGameOver) {
            board.display();
            gameHistory.add(board.copy()); // Ajouter une copie du plateau actuel
            
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
        
        if(save){
            // Exporter l'historique de la partie
            GameStateExporter exporter = new GameStateExporter("game_history.csv");
            exporter.exportGame(gameHistory, board);
        }
        announceWinner();
    }

    /**
     * Joue le prochain coup de la partie
     * @return true si le coup a été joué, false si la partie est terminée
     */
    public boolean playNextMove() {
        if (isGameOver) return false;
        
        if (board.hasValidMoves(currentPlayer.getColor())) {
            Move move = currentPlayer.getMove(board);
            if (move != null) {
                board.makeMove(move);
            }
        } else {
            if (!board.hasValidMoves(currentPlayer.getColor().opposite())) {
                isGameOver = true;
                return false;
            }
        }
        
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        return true;
    }

    /**
     * Affiche le résultat final de la partie.
     * Compte les pions et détermine le vainqueur.
     */
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

    public static void runGames(int numGames) {
        int model1Wins = 0;
        int model2Wins = 0;
        String model1Name = "";
        String model2Name = "";
        int ties = 0;

        for (int i = 0; i < numGames; i++) {
            Board board = new Board();
            Model model1 = ModelRegistry.createModel(0);
            Model model2 = ModelRegistry.createModel(2);
            model1Name = ModelRegistry.getAvailableModels().get(0).name;
            model2Name = ModelRegistry.getAvailableModels().get(2).name;

            GameManager game = new GameManager(board, model1, model2, true);
            while (game.playNextMove()) {
                // Continue playing until the game is over
            }

            int blackCount = game.getBoard().getDiscCount(Disc.BLACK);
            int whiteCount = game.getBoard().getDiscCount(Disc.WHITE);

            if (blackCount > whiteCount) {
                model1Wins++;
            } else if (whiteCount > blackCount) {
                model2Wins++;
            } else {
                ties++;
            }
        }

        System.out.println("Results after " + numGames + " games:");
        System.out.println(model1Name + " model wins: " + model1Wins);
        System.out.println(model2Name + " model wins: " + model2Wins);
        System.out.println("Ties: " + ties);
    }

    public static void main(String[] args) {
        // GameManager game = new GameManager();
        // game.startGame();

        runGames(100);
    }
}

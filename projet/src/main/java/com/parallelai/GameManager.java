package com.parallelai;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.parallelai.export.BoardState;
import com.parallelai.export.GameStateExporter;
import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;
import com.parallelai.players.AIPlayer;
import com.parallelai.players.HumanPlayer;
import com.parallelai.players.RandomAIPlayer;
import com.parallelai.players.UnifiedAIPlayer;
import com.parallelai.models.Model;
import com.parallelai.models.MinimaxModel;
import com.parallelai.models.RandomModel;

/**
 * Gestionnaire de partie d'Othello.
 * Cette classe gère le déroulement d'une partie d'Othello, avec:
 * - Gestion des joueurs (humains ou IA)
 * - Application des règles du jeu
 * - Suivi de l'historique des coups
 * - Export des états de jeu pour l'apprentissage
 */
public class GameManager {
    /** Historique des états du plateau pendant la partie */
    @SuppressWarnings("unused")
    private List<BoardState> gameHistory;
    
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
    
    /** Indique si la partie est terminée */
    private boolean isGameOver;
    
    /** Gestionnaire d'export des états de jeu */
    private GameStateExporter exporter;

    /**
     * Constructeur pour une nouvelle partie interactive.
     * Initialise un nouveau plateau et permet de choisir le mode de jeu.
     */
    public GameManager() {
        this.gameHistory = new ArrayList<>();
        this.scanner = new Scanner(System.in);
        this.board = new Board();
        initializePlayers();
        this.currentPlayer = player1; // Initialisation
        this.exporter = new GameStateExporter("game_states.csv");
    }

    /**
     * Constructeur pour une partie entre deux IA.
     * @param board Le plateau de jeu
     * @param player1 Premier joueur (IA, pions noirs)
     * @param player2 Second joueur (IA, pions blancs)
     */
    public GameManager(Board board, RandomAIPlayer player1, RandomAIPlayer player2) {
        this.gameHistory = new ArrayList<>();
        this.scanner = new Scanner(System.in);
        this.board = board;
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1; // Initialisation
        this.isGameOver = false;
    }
    
    /**
     * Constructor for a game between two models
     * @param board The game board
     * @param model1 Model for player 1
     * @param model2 Model for player 2
     */
    public GameManager(Board board, Model model1, Model model2) {
        this.gameHistory = new ArrayList<>();
        this.scanner = new Scanner(System.in);
        this.board = board;
        this.player1 = new UnifiedAIPlayer(Disc.BLACK, model1);
        this.player2 = new UnifiedAIPlayer(Disc.WHITE, model2);
        this.currentPlayer = player1;
        this.isGameOver = false;
        this.exporter = new GameStateExporter("model_game_states.csv");
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
                player2 = new AIPlayer(Disc.WHITE);
                break;
            case 3:
                player1 = new AIPlayer(Disc.BLACK);
                player2 = new AIPlayer(Disc.WHITE);
                break;
        }
    }

    /**
     * Lance et gère le déroulement de la partie.
     * Alterne les tours entre les joueurs jusqu'à la fin de partie.
     * Affiche le plateau et sauvegarde les états de jeu.
     */
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

    /**
     * Sauvegarde l'état courant du plateau pour l'apprentissage.
     * L'état est exporté avec l'information si la position est gagnante
     * pour le joueur courant.
     */
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

    /**
     * Crée et lance une partie entre deux IA jouant aléatoirement.
     * Utilisé pour générer des données d'apprentissage.
     */
    public static void playRandomAIGame() {
        Board board = new Board();
        RandomAIPlayer player1 = new RandomAIPlayer(Disc.BLACK);
        RandomAIPlayer player2 = new RandomAIPlayer(Disc.WHITE);
        
        GameManager game = new GameManager(board, player1, player2);
        game.startGame();
    }

    /**
     * Creates and plays a game between two models.
     * @param model1 First model (plays black)
     * @param model2 Second model (plays white)
     */
    public static void playModelGame(Model model1, Model model2) {
        Board board = new Board();
        GameManager game = new GameManager(board, model1, model2);
        game.startGame();
    }

    public static void main(String[] args) {
        // Example of how to use the new model game functionality
        Model minimaxModel = new MinimaxModel();
        Model randomModel = new RandomModel();
        playModelGame(minimaxModel, randomModel);
    }
}

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
import com.parallelai.database.FileDatabaseManager;
import com.parallelai.exec.files.FilesUtils;
import java.io.IOException;

/**
 * Gestionnaire de partie d'Othello.
 * Cette classe gère :
 * - L'initialisation et le déroulement des parties
 * - Les différents modes de jeu (Humain vs Humain, Humain vs IA, IA vs IA)
 * - La création et la gestion des joueurs
 * - Le suivi des statistiques de jeu
 */
public class GameManager {
    // Énumération des modes de jeu
    private enum GameMode {
        HUMAN_VS_HUMAN, // Mode joueur contre joueur
        HUMAN_VS_AI, // Mode joueur contre IA
        AI_VS_AI // Mode IA contre IA
    }

    // arguments généraux
    private final Scanner scanner;
    private final Board board;
    private Player player1;
    private Player player2;
    private Player currentPlayer;
    private final List<Board> gameHistory;
    private boolean isGameOver;
    public String save_file = "";

    // Statistiques de jeu
    private String model1Name;
    private String model2Name;
    private int model1Wins;
    private int model2Wins;
    private int ties;

    // Champs pour les statistiques thread-safe
    private final AtomicInteger atomicModel1Wins = new AtomicInteger(0);
    private final AtomicInteger atomicModel2Wins = new AtomicInteger(0);
    private final AtomicInteger atomicTies = new AtomicInteger(0);
    private final AtomicInteger gamesCompleted = new AtomicInteger(0);
    private int totalGames;

    /**
     * Initialise une nouvelle partie avec les paramètres par défaut
     */
    public GameManager() {
        this.scanner = new Scanner(System.in);
        this.board = new Board();
        this.gameHistory = new ArrayList<>();
        this.isGameOver = false;
    }

    /**
     * Initialise une partie avec un plateau et des modèles d'IA spécifiques
     * 
     * @param board  Le plateau de jeu initial
     * @param model1 Modèle d'IA pour le joueur noir
     * @param model2 Modèle d'IA pour le joueur blanc
     */
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
     * Initialise une partie avec des joueurs IA pondérés
     * 
     * @param board Le plateau de jeu initial
     * @param p1    Joueur IA pondéré noir
     * @param p2    Joueur IA pondéré blanc
     */
    public GameManager(Board board, AIWeightedPlayer p1, AIWeightedPlayer p2) { // TODO : maybe unused
        this.scanner = new Scanner(System.in);
        this.board = board;
        this.player1 = p1;
        this.player2 = p2;
        this.currentPlayer = player1;
        this.gameHistory = new ArrayList<>();
        this.isGameOver = false;
    }

    /**
     * Initialise et démarre la partie selon le mode choisi
     */
    public void initialize() {
        GameMode gameMode = promptGameMode();
        setupPlayers(gameMode);

        if (gameMode != GameMode.AI_VS_AI) {
            startGame();
        }
    }

    /**
     * Affiche le menu de sélection du mode de jeu
     * 
     * @return Le mode de jeu sélectionné
     */
    private GameMode promptGameMode() {
        System.out.println("Choisissez un mode de jeu :");
        System.out.println("1. Humain contre Humain");
        System.out.println("2. Humain contre IA");
        System.out.println("3. IA contre IA");

        int choice = scanner.nextInt();
        return switch (choice) {
            case 1 -> GameMode.HUMAN_VS_HUMAN;
            case 2 -> GameMode.HUMAN_VS_AI;
            case 3 -> GameMode.AI_VS_AI;
            default -> throw new IllegalArgumentException("Mode de jeu invalide");
        };
    }

    /**
     * Configure les joueurs selon le mode de jeu sélectionné
     * 
     * @param gameMode Le mode de jeu choisi
     */
    private void setupPlayers(GameMode gameMode) {
        switch (gameMode) {
            case HUMAN_VS_HUMAN:
                player1 = new HumanPlayer(Disc.BLACK, scanner);
                player2 = new HumanPlayer(Disc.WHITE, scanner);
                break;
            case HUMAN_VS_AI:
                player1 = new HumanPlayer(Disc.BLACK, scanner);
                Model aiModel = selectAIModel("Sélectionnez le modèle d'IA pour le joueur blanc");
                player2 = new AIPlayer(Disc.WHITE, aiModel);
                break;
            case AI_VS_AI:
                handleAIGame();
                break;
        }
        currentPlayer = player1;
    }

    /**
     * Gère la configuration et l'exécution des parties IA contre IA
     */
    private void handleAIGame() {
        AIType aiType = promptAIType();
        System.out.println("Entrez le nombre de parties (1 pour une seule partie) :");
        int numGames = scanner.nextInt();
        totalGames = numGames;

        Model model1 = selectAIModel("Sélectionnez le modèle d'IA pour le joueur noir");
        Model model2 = selectAIModel("Sélectionnez le modèle d'IA pour le joueur blanc");

        // Crée les joueurs IA en fonction du type sélectionné et du modèle
        if (aiType == AIType.REGULAR) {
            player1 = new AIPlayer(Disc.BLACK, model1);
            player2 = new AIPlayer(Disc.WHITE, model2);
        } else {
            player1 = new AIWeightedPlayer(Disc.BLACK, model1);
            player2 = new AIWeightedPlayer(Disc.WHITE, model2);
        }
        currentPlayer = player1;

        System.out.println("Voulez-vous sauvegarder les parties ? (y/n)");
        scanner.nextLine();
        String choice = scanner.nextLine().toLowerCase();

        if (choice.equals("y")) {
            DataSetManager manager = new DataSetManager(model1, model2, totalGames, aiType);
            manager.initializeDatasetOptions();
        } else {
            runMultipleGames(numGames, model1, model2, aiType);
        }
    }

    /**
     * Demande à l'utilisateur de choisir le type d'IA
     * 
     * @return Le type d'IA sélectionné
     */
    private AIType promptAIType() {
        System.out.println("Choisissez le type d'IA :");
        System.out.println("1. IA régulière - Meilleurs coups");
        System.out.println("2. IA pondérée - Sélection aléatoire pondérée");

        return scanner.nextInt() == 1 ? AIType.REGULAR : AIType.WEIGHTED;
    }

    /**
     * Exécute plusieurs parties et affiche les statistiques
     * 
     * @param numGames Nombre de parties à jouer
     * @param model1   Premier modèle d'IA
     * @param model2   Deuxième modèle d'IA
     * @param aiType   Type d'IA à utiliser
     */
    private void runMultipleGames(int numGames, Model model1, Model model2, AIType aiType) {
        model1Name = model1.getName();
        model2Name = model2.getName();

        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(processors);
        List<Future<GameResult>> futures = new ArrayList<>();

        System.out.println("Progression : ");

        // Soumettre toutes les parties à l'exécuteur
        for (int i = 0; i < numGames; i++) {
            GameRunner runner = new GameRunner(model1, model2, aiType, () -> {
                gamesCompleted.incrementAndGet();
                updateProgressBar();
            });
            futures.add(executor.submit(runner));
        }

        // Collecter les résultats
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

        System.out.println(); // Nouvelle ligne après la barre de progression

        // Mise à jour des variables de statistiques originales pour la compatibilité
        model1Wins = atomicModel1Wins.get();
        model2Wins = atomicModel2Wins.get();
        ties = atomicTies.get();

        displayGameStatistics(numGames);

        // Nettoyer les répertoires des modèles après la fin des parties
        try {
            FilesUtils.clearModelDirectories();
        } catch (IOException e) {
            System.err.println("Failed to clear model directories: " + e.getMessage());
        }
    }

    /**
     * Affiche les statistiques finales des parties
     * 
     * @param numGames Nombre total de parties jouées
     */
    private void displayGameStatistics(int numGames) {
        System.out.println("Résultats après " + numGames + " parties :");
        System.out.println(model1Name + " (black) victoires : " + model1Wins);
        System.out.println(model2Name + " (white) victoires : " + model2Wins);
        System.out.println("Matchs nuls : " + ties);
    }

    /**
     * Permet à l'utilisateur de sélectionner un modèle d'IA
     * 
     * @param prompt Message à afficher pour la sélection
     * @return Le modèle d'IA sélectionné
     */
    private Model selectAIModel(String prompt) {
        List<ModelRegistry.ModelInfo> models = ModelRegistry.getAvailableModels();
        System.out.println(prompt + " :");

        // Affiche les types de modèles disponibles
        for (int i = 0; i < models.size(); i++) {
            System.out.println((i + 1) + ". " + models.get(i).name);
        }

        int modelTypeChoice = scanner.nextInt();
        if (modelTypeChoice < 1 || modelTypeChoice > models.size()) {
            throw new IllegalArgumentException("Choix de type de modèle invalide");
        }

        // Récupère le type de modèle
        String modelType = models.get(modelTypeChoice - 1).name;

        // Passe la sélection de base de données pour le modèle Aléatoire
        if (modelType.equals("Random")) {
            return ModelRegistry.createModel(modelTypeChoice - 1, "random");
        }

        // Pour les modèles CNN et Dense, sélection depuis la base de données
        int dbType = modelType.equals("CNN") ? 1 : 2;

        // Affiche les modèles disponibles dans la base de données
        String[] availableModels = FileDatabaseManager.getFileList(dbType);
        if (availableModels.length == 0) {
            throw new RuntimeException("Aucun modèle " + modelType + " disponible dans la base de données");
        }

        System.out.println("\nModèles " + modelType + " disponibles :");
        for (int i = 0; i < availableModels.length; i++) {
            System.out.println((i + 1) + ". " + availableModels[i]);
        }

        // Obtient le choix du modèle
        System.out.println("Sélectionnez un modèle (1-" + availableModels.length + ") :");
        int modelChoice = scanner.nextInt();
        if (modelChoice < 1 || modelChoice > availableModels.length) {
            throw new IllegalArgumentException("Choix de modèle invalide");
        }

        String selectedModelName = availableModels[modelChoice - 1];
        return ModelRegistry.createModel(modelTypeChoice - 1, selectedModelName);
    }

    /**
     * Démarre une partie avec option de sauvegarde
     * 
     * @param save True pour sauvegarder la partie, False sinon
     */
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

    /**
     * Démarre une partie sans sauvegarde
     */
    public void startGame() {
        startGame(false);
    }

    /**
     * Traite le prochain coup de la partie
     * 
     * @return False si la partie est terminée, True sinon
     */
    private boolean processNextMove() {
        if (!board.hasValidMoves(currentPlayer.getColor())) {
            if (player1 instanceof HumanPlayer || player2 instanceof HumanPlayer) {
                System.out.println("Aucun coup valide pour " + currentPlayer.getColor());
            }
            if (!board.hasValidMoves(currentPlayer.getColor().opposite())) {
                isGameOver = true;
                return false;
            }
            return true;
        }
        if (player1 instanceof HumanPlayer || player2 instanceof HumanPlayer) {
            System.out.println("Joueur actuel : " + currentPlayer.getColor());
        }
        Move move = currentPlayer.getMove(board);
        if (move != null) {
            board.makeMove(move);
        }
        return true;
    }

    /**
     * Joue le prochain coup de la partie
     * 
     * @return False si la partie est terminée, True sinon
     */
    public boolean playNextMove() {
        if (isGameOver)
            return false;

        if (!processNextMove()) {
            return false;
        }

        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        return true;
    }

    /**
     * Annonce le gagnant de la partie
     */
    private void announceWinner() {
        board.display();
        int blackCount = board.getDiscCount(Disc.BLACK);
        int whiteCount = board.getDiscCount(Disc.WHITE);

        System.out.println("Partie terminée !");
        System.out.println("Noir : " + blackCount);
        System.out.println("Blanc : " + whiteCount);

        if (blackCount > whiteCount) {
            System.out.println("Noir gagne !");
        } else if (whiteCount > blackCount) {
            System.out.println("Blanc gagne !");
        } else {
            System.out.println("Match nul !");
        }
    }

    /**
     * Récupère le plateau de jeu actuel
     * 
     * @return Le plateau de jeu
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Vérifie si la partie est terminée
     * 
     * @return True si la partie est terminée, False sinon
     */
    public boolean isGameOver() {
        return isGameOver;
    }

    /**
     * Met à jour la barre de progression
     */
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

    public static void main(String[] args) {
        GameManager game = new GameManager();
        game.initialize();
    }
}

package com.parallelai.exec.play;

import com.parallelai.database.FileDatabaseManager;
import com.parallelai.export.implementations.ClassicThreadExporter;
import com.parallelai.models.utils.Model;
import com.parallelai.players.AIPlayer;
import com.parallelai.players.AIWeightedPlayer;
import com.parallelai.game.Disc;
import com.parallelai.exec.files.FilesUtils;
import com.parallelai.exec.play.GameRunner.AIType;

import java.io.File;
import java.util.Scanner;

/**
 * Gestionnaire de jeux de données pour l'apprentissage des modèles d'IA.
 * Cette classe permet de :
 * - Créer de nouveaux jeux de données à partir de parties simulées
 * - Ajouter des données à des jeux existants
 * - Gérer le stockage local et distant des données
 * - Interfacer avec différents types d'IA (standard et pondérée)
 */
public class DataSetManager {
    /** Chemin du dossier temporaire pour le stockage local des données */
    private static final String DATA_FOLDER = "projet\\src\\main\\ressources\\data\\";

    /** Scanner pour la lecture des entrées utilisateur */
    private final Scanner scanner;

    /** Premier modèle d'IA utilisé pour les simulations */
    private final Model model1;

    /** Second modèle d'IA utilisé pour les simulations */
    private final Model model2;

    /** Nombre de parties à simuler */
    private final int nbParties;

    /** Type d'IA à utiliser (standard ou pondérée) */
    private final AIType aiType;

    /**
     * Constructeur du gestionnaire de jeux de données
     * 
     * @param model1    Premier modèle d'IA pour les simulations
     * @param model2    Second modèle d'IA pour les simulations
     * @param nbParties Nombre de parties à simuler
     * @param aiType    Type d'IA à utiliser (REGULAR ou WEIGHTED)
     */
    public DataSetManager(Model model1, Model model2, int nbParties, AIType aiType) {
        this.scanner = new Scanner(System.in);
        this.model1 = model1;
        this.model2 = model2;
        this.nbParties = nbParties;
        this.aiType = aiType;
    }

    /**
     * Affiche et gère le menu des options de gestion des jeux de données.
     * Permet à l'utilisateur de :
     * - Créer un nouveau jeu de données
     * - Ajouter des données à un jeu existant
     * - Annuler l'opération
     */
    public void initializeDatasetOptions() {
        System.out.println("\n=== Options du Jeu de Données ===");
        System.out.println("1. Créer un nouveau jeu de données");
        System.out.println("2. Ajouter à un jeu de données existant");
        System.out.println("0. Annuler");

        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        switch (choice) {
            case 1:
                createNewDataset();
                break;
            case 2:
                addToExistingDataset();
                break;
            default:
                System.out.println("Opération annulée.");
                break;
        }
    }

    /**
     * Crée un nouveau jeu de données en simulant des parties.
     * Processus :
     * 1. Vérifie et crée le dossier de données si nécessaire
     * 2. Demande le nom du nouveau jeu de données
     * 3. Lance les simulations avec le type d'IA spécifié
     * 4. Sauvegarde les résultats dans la base de données
     * 5. Nettoie les fichiers temporaires
     */
    private void createNewDataset() {
        // Vérifier si le dossier existe, sinon le créer
        File dataFolder = new File(DATA_FOLDER);
        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                System.out.println("Dossier de données créé : " + DATA_FOLDER);
            } else {
                System.err.println("Erreur lors de la création du dossier de données");
                return;
            }
        }

        System.out.println("\nEntrez le nom de votre jeu de données (sans l'extension .csv) :");
        String datasetName = scanner.nextLine();
        String fullPath = DATA_FOLDER + datasetName + ".csv";

        int nbThreads = Runtime.getRuntime().availableProcessors();
        ClassicThreadExporter exporter = new ClassicThreadExporter(fullPath);

        System.out.println("\nInitialisation des modèles d'IA...");
        System.out.println("Cette étape peut prendre quelques instants pour les modèles CNN/MLP...");

        if (aiType == AIType.REGULAR) {
            AIPlayer p1 = new AIPlayer(Disc.BLACK, model1);
            AIPlayer p2 = new AIPlayer(Disc.WHITE, model2);
            System.out.println("Modèles initialisés. Début de la génération du jeu de données...\n");
            exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
        } else {
            AIWeightedPlayer p1 = new AIWeightedPlayer(Disc.BLACK, model1);
            AIWeightedPlayer p2 = new AIWeightedPlayer(Disc.WHITE, model2);
            System.out.println("Modèles initialisés. Début de la génération du jeu de données...\n");
            exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
        }
        System.out.println("\nChargement du jeu de données dans la base...");
        FileDatabaseManager.insertFile(fullPath, 3);

        // Cleanup

        try {
            if (new File(fullPath).delete()) {
                System.out.println("Fichier local nettoyé avec succès.");
                FilesUtils.clearModelDirectories();
            }
        } catch (Exception e) {
            System.err.println("Attention : Impossible de supprimer le fichier local : " + e.getMessage());
        }

        System.out.println("\nCréation du jeu de données terminée !");
    }

    /**
     * Ajoute de nouvelles données à un jeu existant.
     * Processus :
     * 1. Permet la sélection d'un jeu existant
     * 2. Télécharge les données existantes
     * 3. Simule de nouvelles parties
     * 4. Fusionne les nouvelles données avec les existantes
     * 5. Met à jour la base de données
     * 6. Nettoie les fichiers temporaires
     */
    private void addToExistingDataset() {
        String selectedDataset = selectExistingDataset();
        if (selectedDataset == null) {
            return;
        }

        String localPath = DATA_FOLDER + selectedDataset;

        try {
            System.out.println("Téléchargement du jeu de données existant...");
            FileDatabaseManager.downloadFile(selectedDataset, 3);

            int nbThreads = Runtime.getRuntime().availableProcessors();
            ClassicThreadExporter exporter = new ClassicThreadExporter(localPath);

            System.out.println("\nInitialisation des modèles d'IA...");
            System.out.println("Cette étape peut prendre quelques instants pour les modèles CNN/MLP...");

            if (aiType == AIType.REGULAR) {
                AIPlayer p1 = new AIPlayer(Disc.BLACK, model1);
                AIPlayer p2 = new AIPlayer(Disc.WHITE, model2);
                System.out.println("Modèles initialisés. Début de l'ajout des parties...\n");
                exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
            } else {
                AIWeightedPlayer p1 = new AIWeightedPlayer(Disc.BLACK, model1);
                AIWeightedPlayer p2 = new AIWeightedPlayer(Disc.WHITE, model2);
                System.out.println("Modèles initialisés. Début de l'ajout des parties...\n");
                exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
            }

            System.out.println("\nMise à jour du jeu de données dans la base...");
            FileDatabaseManager.deleteFile(selectedDataset, 3);
            FileDatabaseManager.insertFile(localPath, 3);

            if (new File(localPath).delete()) {
                System.out.println("Fichier local nettoyé avec succès.");
            }

            System.out.println("\nMise à jour du jeu de données terminée !");

        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du jeu de données : " + e.getMessage());
        } finally {
            try {
                new File(localPath).delete();
            } catch (Exception e) {
                System.err.println("Attention : Impossible de supprimer le fichier local : " + e.getMessage());
            }
        }
    }

    /**
     * Gère l'interface de sélection des jeux de données existants.
     * Fonctionnalités :
     * - Affiche la liste des jeux disponibles
     * - Permet la sélection par numéro
     * - Gère les erreurs de saisie
     * - Offre une option de retour
     * 
     * @return Le nom du jeu de données sélectionné ou null si annulation
     */
    private String selectExistingDataset() {
        while (true) {
            System.out.println("\nSélectionnez un jeu de données :");
            String[] datasets = FileDatabaseManager.getFileList(3);

            if (datasets.length == 0) {
                System.out.println("Aucun jeu de données disponible.");
                return null;
            }

            // Afficher la liste numérotée des datasets
            for (int i = 0; i < datasets.length; i++) {
                System.out.println((i + 1) + ". " + datasets[i]);
            }
            System.out.println("0. Retour au menu précédent");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                return null;
            }

            if (choice > 0 && choice <= datasets.length) {
                return datasets[choice - 1];
            }

            System.out.println("Sélection invalide. Veuillez réessayer.");
        }
    }
}

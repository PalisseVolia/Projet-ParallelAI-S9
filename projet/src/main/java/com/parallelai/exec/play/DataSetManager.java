package com.parallelai.exec.play;

import com.parallelai.database.FileDatabaseManager;
import com.parallelai.export.implementations.ClassicThreadExporter;
import com.parallelai.models.RandomModel;
import com.parallelai.models.utils.Model;
import com.parallelai.models.utils.ModelRegistry;
import com.parallelai.players.AIPlayer;
import com.parallelai.players.AIWeightedPlayer;
import com.parallelai.game.Disc;
import com.parallelai.game.Player;
import com.parallelai.exec.play.GameRunner.AIType;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.io.File;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("unused")
/**
 * Gestionnaire de jeux de données pour l'apprentissage des modèles.
 * Permet de jouer des parties et optionnellement les sauvegarder dans des jeux de données.
 */
public class DataSetManager {
    // Dossier où sont stockés temporairement les fichiers de données
    private static final String DATA_FOLDER = "projet\\src\\main\\ressources\\data\\";
    private final Scanner scanner;
    private final Model model1;
    private final Model model2;
    private final int nbParties;
    private final AIType aiType;
    private final Player player1;
    private final Player player2;

    /**
     * Constructeur avec les paramètres nécessaires
     */
    public DataSetManager(Model model1, Model model2, int nbParties, AIType aiType) {
        this.scanner = new Scanner(System.in);
        this.model1 = model1;
        this.model2 = model2;
        this.nbParties = nbParties;
        this.aiType = aiType;
        
        // Création des players selon le type d'AI
        if (aiType == AIType.WEIGHTED) {
            this.player1 = new AIWeightedPlayer(Disc.BLACK, model1);
            this.player2 = new AIWeightedPlayer(Disc.WHITE, model2);
        } else {
            this.player1 = new AIPlayer(Disc.BLACK, model1);
            this.player2 = new AIPlayer(Disc.WHITE, model2);
        }
    }

    /**
     * méthode d'initialisation qui demande d'abord si on veut sauvegarder
     */
    public void initialize() {
        System.out.println("\n=== Gestionnaire de Parties ===");
        System.out.println("Voulez-vous sauvegarder les parties ? (o/n)");
        String choice = scanner.nextLine().toLowerCase();

        if (choice.equals("o")) {
            initializeDatasetOptions();
        } else {
            playGamesWithoutSaving();
        }
    }

    /**
     * Joue les parties sans sauvegarde
     */
    private void playGamesWithoutSaving() {
        System.out.println("\nJeu de " + nbParties + " parties en cours...");
        // Utiliser les Players directement sans sauvegarder
        ClassicThreadExporter exporter = new ClassicThreadExporter(null);
        int nbThreads = Runtime.getRuntime().availableProcessors();
        if (aiType == AIType.REGULAR) {
            AIPlayer p1 = new AIPlayer(Disc.BLACK, model1);
            AIPlayer p2 = new AIPlayer(Disc.WHITE, model2);
            exporter.startGamesNoSave(nbParties, p1, p2, nbThreads);
        } else {
            AIWeightedPlayer p1 = new AIWeightedPlayer(Disc.BLACK, model1);
            AIWeightedPlayer p2 = new AIWeightedPlayer(Disc.WHITE, model2);
            exporter.startGamesNoSave(nbParties, p1, p2, nbThreads);
        }
        System.out.println("Parties terminées !");
    }

    /**
     * Initialize les options de dataset quand on veut sauvegarder
     */
    private void initializeDatasetOptions() {
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
     * Version modifiée de createNewDataset qui utilise les Players fournis
     */
    private void createNewDataset() {
        System.out.println("\nEntrez le nom de votre jeu de données (sans l'extension .csv) :");
        String datasetName = scanner.nextLine();
        String fullPath = DATA_FOLDER + datasetName + ".csv";
        
        int nbThreads = Runtime.getRuntime().availableProcessors();
        ClassicThreadExporter exporter = new ClassicThreadExporter(fullPath);
        
        System.out.println("\nGénération du jeu de données...");
        if (aiType == AIType.REGULAR) {
            AIPlayer p1 = new AIPlayer(Disc.BLACK, model1);
            AIPlayer p2 = new AIPlayer(Disc.WHITE, model2);
            exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
        } else {
            AIWeightedPlayer p1 = new AIWeightedPlayer(Disc.BLACK, model1);
            AIWeightedPlayer p2 = new AIWeightedPlayer(Disc.WHITE, model2);
            exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
        }        
        System.out.println("\nChargement du jeu de données dans la base...");
        FileDatabaseManager.insertFile(fullPath, 3);
        
        // Cleanup
        try {
            if (new File(fullPath).delete()) {
                System.out.println("Fichier local nettoyé avec succès.");
            }
        } catch (Exception e) {
            System.err.println("Attention : Impossible de supprimer le fichier local : " + e.getMessage());
        }
        
        System.out.println("\nCréation du jeu de données terminée !");
    }

    /**
     * Version modifiée de addToExistingDataset qui utilise les Players fournis
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
            
            System.out.println("\nAjout de nouvelles parties au jeu de données...");
            
            if (aiType == AIType.REGULAR) {
                AIPlayer p1 = new AIPlayer(Disc.BLACK, model1);
                AIPlayer p2 = new AIPlayer(Disc.WHITE, model2);
                exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
            } else {
                AIWeightedPlayer p1 = new AIWeightedPlayer(Disc.BLACK, model1);
                AIWeightedPlayer p2 = new AIWeightedPlayer(Disc.WHITE, model2);
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
     * Affiche la liste des datasets existants et permet à l'utilisateur d'en sélectionner un
     * @return Le nom du dataset sélectionné ou null si l'utilisateur annule
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
            scanner.nextLine(); // Consume newline
            
            if (choice == 0) {
                return null;
            }
            
            if (choice > 0 && choice <= datasets.length) {
                return datasets[choice - 1];
            }
            
            System.out.println("Sélection invalide. Veuillez réessayer.");
        }
    }

    /**
     * Permet de télécharger un dataset existant vers un emplacement choisi par l'utilisateur
     * Utilise une interface graphique pour sélectionner l'emplacement de sauvegarde
     */
    private void downloadDataset() {
        String selectedDataset = selectExistingDataset();
        if (selectedDataset == null) {
            return;
        }

        System.out.println("Jeu de données sélectionné : " + selectedDataset);

        try {
            // Créer et configurer le JFileChooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choisissez où sauvegarder le jeu de données");
            fileChooser.setSelectedFile(new File(selectedDataset));
            
            // Créer une fenêtre parent invisible pour le dialog
            javax.swing.JFrame frame = new javax.swing.JFrame();
            frame.setAlwaysOnTop(true);
            
            // Afficher le dialog et attendre la sélection
            int result = fileChooser.showSaveDialog(frame);
            frame.dispose();
            
            if (result == JFileChooser.APPROVE_OPTION) {
                String savePath = fileChooser.getSelectedFile().getAbsolutePath();
                
                // Ajouter l'extension .csv si nécessaire
                if (!savePath.toLowerCase().endsWith(".csv")) {
                    savePath += ".csv";
                }
                
                System.out.println("Téléchargement du jeu de données vers : " + savePath);
                FileDatabaseManager.downloadFile(selectedDataset, savePath, 3);
                System.out.println("Téléchargement terminé !");
            } else {
                System.out.println("Téléchargement annulé.");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sélection du fichier : " + e.getMessage());
        }
    }

    /**
     * Affiche la liste des modèles disponibles et permet à l'utilisateur d'en sélectionner un
     * @return Le modèle sélectionné
     */
    private Model selectModel() {
        List<ModelRegistry.ModelInfo> models = ModelRegistry.getAvailableModels();
        for (int i = 0; i < models.size(); i++) {
            System.out.println((i + 1) + ". " + models.get(i).name);
        }
        int modelChoice = scanner.nextInt();
        return ModelRegistry.createModel(modelChoice - 1);
    }
}

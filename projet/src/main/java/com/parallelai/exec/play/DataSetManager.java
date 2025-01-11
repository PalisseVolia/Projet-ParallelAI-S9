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
 * Permet de jouer des parties et optionnellement les sauvegarder dans des datasets.
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
        System.out.println("\n=== Game Manager ===");
        System.out.println("Do you want to save the games? (y/n)");
        String choice = scanner.nextLine().toLowerCase();

        if (choice.equals("y")) {
            initializeDatasetOptions();
        } else {
            playGamesWithoutSaving();
        }
    }

    /**
     * Joue les parties sans sauvegarde
     */
    private void playGamesWithoutSaving() {
        System.out.println("\nPlaying " + nbParties + " games...");
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
        System.out.println("Games completed!");
    }

    /**
     * Initialize les options de dataset quand on veut sauvegarder
     */
    private void initializeDatasetOptions() {
        System.out.println("\n=== Dataset Options ===");
        System.out.println("1. Create new dataset");
        System.out.println("2. Add to existing dataset");
        System.out.println("0. Cancel");
        
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
                System.out.println("Operation cancelled.");
                break;
        }
    }

    /**
     * Version modifiée de createNewDataset qui utilise les Players fournis
     */
    private void createNewDataset() {
        System.out.println("\nEnter the name for your dataset (without .csv extension):");
        String datasetName = scanner.nextLine();
        String fullPath = DATA_FOLDER + datasetName + ".csv";
        
        int nbThreads = Runtime.getRuntime().availableProcessors();
        ClassicThreadExporter exporter = new ClassicThreadExporter(fullPath);
        
        System.out.println("\nGenerating dataset...");
        if (aiType == AIType.REGULAR) {
            AIPlayer p1 = new AIPlayer(Disc.BLACK, model1);
            AIPlayer p2 = new AIPlayer(Disc.WHITE, model2);
            exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
        } else {
            AIWeightedPlayer p1 = new AIWeightedPlayer(Disc.BLACK, model1);
            AIWeightedPlayer p2 = new AIWeightedPlayer(Disc.WHITE, model2);
            exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
        }        
        System.out.println("\nUploading dataset to database...");
        FileDatabaseManager.insertFile(fullPath, 3);
        
        // Cleanup
        try {
            if (new File(fullPath).delete()) {
                System.out.println("Local file cleaned up successfully.");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not delete local file: " + e.getMessage());
        }
        
        System.out.println("\nDataset creation completed!");
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
            System.out.println("Downloading existing dataset...");
            FileDatabaseManager.downloadFile(selectedDataset, 3);
            
            int nbThreads = Runtime.getRuntime().availableProcessors();
            ClassicThreadExporter exporter = new ClassicThreadExporter(localPath);
            
            System.out.println("\nAdding new games to dataset...");
            
            if (aiType == AIType.REGULAR) {
                AIPlayer p1 = new AIPlayer(Disc.BLACK, model1);
                AIPlayer p2 = new AIPlayer(Disc.WHITE, model2);
                exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
            } else {
                AIWeightedPlayer p1 = new AIWeightedPlayer(Disc.BLACK, model1);
                AIWeightedPlayer p2 = new AIWeightedPlayer(Disc.WHITE, model2);
                exporter.startGamesWithUniqueStatesClassicThreads(nbParties, p1, p2, nbThreads, false);
            }

            System.out.println("\nUpdating dataset in database...");
            FileDatabaseManager.deleteFile(selectedDataset, 3);
            FileDatabaseManager.insertFile(localPath, 3);
            
            if (new File(localPath).delete()) {
                System.out.println("Local file cleaned up successfully.");
            }
            
            System.out.println("\nDataset update completed!");
            
        } catch (Exception e) {
            System.err.println("Error during dataset update: " + e.getMessage());
        } finally {
            try {
                new File(localPath).delete();
            } catch (Exception e) {
                System.err.println("Warning: Could not delete local file: " + e.getMessage());
            }
        }
    }

    /**
     * Affiche la liste des datasets existants et permet à l'utilisateur d'en sélectionner un
     * @return Le nom du dataset sélectionné ou null si l'utilisateur annule
     */
    private String selectExistingDataset() {
        while (true) {
            System.out.println("\nSelect a dataset:");
            String[] datasets = FileDatabaseManager.getFileList(3);
            
            if (datasets.length == 0) {
                System.out.println("No datasets available.");
                return null;
            }
            
            // Afficher la liste numérotée des datasets
            for (int i = 0; i < datasets.length; i++) {
                System.out.println((i + 1) + ". " + datasets[i]);
            }
            System.out.println("0. Return to previous menu");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            if (choice == 0) {
                return null;
            }
            
            if (choice > 0 && choice <= datasets.length) {
                return datasets[choice - 1];
            }
            
            System.out.println("Invalid selection. Please try again.");
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

        System.out.println("Selected dataset: " + selectedDataset);

        try {
            // Créer et configurer le JFileChooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose where to save the dataset");
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
                
                System.out.println("Downloading dataset to: " + savePath);
                FileDatabaseManager.downloadFile(selectedDataset, savePath, 3);
                System.out.println("Download completed!");
            } else {
                System.out.println("Download cancelled.");
            }
        } catch (Exception e) {
            System.err.println("Error during file selection: " + e.getMessage());
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

    /**
     * Méthode main modifiée pour utiliser AIType
     */
    public static void main(String[] args) {
        Model model1 = new RandomModel();
        Model model2 = new RandomModel();
        int nbParties = 1000;
        
        System.out.println("Choose AI type:");
        System.out.println("1. Regular AI");
        System.out.println("2. Weighted AI");
        
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        AIType aiType = (choice == 2) ? AIType.WEIGHTED : AIType.REGULAR;
        
        DataSetManager manager = new DataSetManager(model1, model2, nbParties, aiType);
        manager.initialize();
        
        scanner.close();
    }
}

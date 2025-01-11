package com.parallelai.exec.data;

import com.parallelai.database.FileDatabaseManager;
import com.parallelai.export.implementations.ClassicThreadExporter;
import com.parallelai.models.utils.Model;
import com.parallelai.models.utils.ModelRegistry;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.io.File;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("unused")
public class DatasetManager {
    private static final String DATA_FOLDER = "projet\\src\\main\\ressources\\data\\";
    private final Scanner scanner;

    public DatasetManager() {
        this.scanner = new Scanner(System.in);
    }

    public void initialize() {
        while (true) {
            System.out.println("\n=== Dataset Manager ===");
            System.out.println("Choose an option:");
            System.out.println("1. Create new dataset");
            System.out.println("2. Add data to existing dataset");
            System.out.println("3. See existing datasets");
            System.out.println("4. Download dataset");
            System.out.println("5. Exit");
            System.out.println("0. Exit to previous menu");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            switch (choice) {
                case 0:
                    return;
                case 1:
                    createNewDataset();
                    break;
                case 2:
                    addToExistingDataset();
                    break;
                case 3:
                    System.out.println("\nExisting datasets in database:");
                    FileDatabaseManager.listFiles(3); // 3 pour type dataset
                    break;
                case 4:
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        }
    }

    private void createNewDataset() {
        // Get dataset name
        System.out.println("\nEnter the name for your dataset (without .csv extension):");
        String datasetName = scanner.nextLine();
        String fullPath = DATA_FOLDER + datasetName + ".csv";
        
        // Get number of games
        System.out.println("Enter the number of games to play:");
        int numGames = scanner.nextInt();
        
        // Select models
        System.out.println("\nSelect first model (Black):");
        Model model1 = selectModel();
        
        System.out.println("\nSelect second model (White):");
        Model model2 = selectModel();
        
        // Number of threads
        int nbThreads = Runtime.getRuntime().availableProcessors();
        
        // Create and configure the exporter
        ClassicThreadExporter exporter = new ClassicThreadExporter(fullPath);
        
        System.out.println("\nGenerating dataset...");
        exporter.startGamesWithUniqueStatesClassicThreads(numGames, model1, model2, nbThreads, false);
        
        // Upload to database
        System.out.println("\nUploading dataset to database...");
        FileDatabaseManager.insertFile(fullPath, 3); // 3 represents dataset type
        
        // Cleanup local file
        try {
            if (new File(fullPath).delete()) {
                System.out.println("Local file cleaned up successfully.");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not delete local file: " + e.getMessage());
        }
        
        System.out.println("\nDataset creation completed!");
    }

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

    private void addToExistingDataset() {
        String selectedDataset = selectExistingDataset();
        if (selectedDataset == null) {
            return;
        }
        
        System.out.println("Selected dataset: " + selectedDataset);
        String localPath = DATA_FOLDER + selectedDataset;
        
        try {
            // Télécharger le dataset existant
            System.out.println("Downloading existing dataset...");
            FileDatabaseManager.downloadFile(selectedDataset, 3);
            
            // Configuration habituelle
            System.out.println("\nEnter the number of additional games to play:");
            int numGames = scanner.nextInt();
            
            System.out.println("\nSelect first model (Black):");
            Model model1 = selectModel();
            
            System.out.println("\nSelect second model (White):");
            Model model2 = selectModel();
            
            int nbThreads = Runtime.getRuntime().availableProcessors();
            ClassicThreadExporter exporter = new ClassicThreadExporter(localPath);
            
            // Ajouter les nouvelles données
            System.out.println("\nAdding new games to dataset...");
            exporter.startGamesWithUniqueStatesClassicThreads(numGames, model1, model2, nbThreads, true);
            
            // Supprimer l'ancienne version puis insérer la nouvelle
            System.out.println("\nUpdating dataset in database...");
            FileDatabaseManager.deleteFile(selectedDataset, 3);
            FileDatabaseManager.insertFile(localPath, 3);
            
            // Nettoyage
            if (new File(localPath).delete()) {
                System.out.println("Local file cleaned up successfully.");
            }
            
            System.out.println("\nDataset update completed!");
            
        } catch (Exception e) {
            System.err.println("Error during dataset update: " + e.getMessage());
        } finally {
            // Nettoyage de sécurité
            try {
                new File(localPath).delete();
            } catch (Exception e) {
                System.err.println("Warning: Could not delete local file: " + e.getMessage());
            }
        }
    }

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

    private Model selectModel() {
        List<ModelRegistry.ModelInfo> models = ModelRegistry.getAvailableModels();
        for (int i = 0; i < models.size(); i++) {
            System.out.println((i + 1) + ". " + models.get(i).name);
        }
        int modelChoice = scanner.nextInt();
        return ModelRegistry.createModel(modelChoice - 1);
    }

    public static void main(String[] args) {
        DatasetManager manager = new DatasetManager();
        manager.initialize();
    }
}

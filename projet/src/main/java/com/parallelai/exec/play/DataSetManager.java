package com.parallelai.exec.play;

import com.parallelai.database.FileDatabaseManager;
import com.parallelai.export.implementations.ClassicThreadExporter;
import com.parallelai.models.utils.Model;
import com.parallelai.models.utils.ModelRegistry;

import java.util.List;
import java.util.Scanner;

public class DataSetManager {
    private static final String DATA_FOLDER = "projet\\src\\main\\ressources\\data\\";
    private final Scanner scanner;

    public DataSetManager() {
        this.scanner = new Scanner(System.in);
    }

    public void initialize() {
        while (true) {
            System.out.println("\n=== Dataset Manager ===");
            System.out.println("Choose an option:");
            System.out.println("1. Create new dataset");
            System.out.println("2. Add data to existing dataset");
            System.out.println("3. See existing datasets");
            System.out.println("4. Exit");
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
        // Execute the games and generate the dataset
        exporter.startGamesWithUniqueStatesClassicThreads(numGames, model1, model2, nbThreads);
        
        // Upload to database
        System.out.println("\nUploading dataset to database...");
        FileDatabaseManager.insertFile(fullPath, 3); // 3 represents dataset type
        
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
            return; // User chose to return to previous menu
        }
        
        System.out.println("Selected dataset: " + selectedDataset);
        // TODO: Implémenter la logique d'ajout de données
        // La suite sera implémentée dans la prochaine étape
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
        DataSetManager manager = new DataSetManager();
        manager.initialize();
    }
}

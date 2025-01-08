package com.parallelai.exec.train;

import com.parallelai.training.CnnTraining;
import com.parallelai.training.DenseTraining;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TrainerManager {
    private static final String DATASET_DIR = "projet\\src\\main\\ressources\\data";
    
    private String selectDataset(Scanner scanner) {
        File dataDir = new File(DATASET_DIR);
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            throw new RuntimeException("Dataset directory not found at: " + dataDir.getAbsolutePath());
        }

        List<File> datasets = Arrays.stream(dataDir.listFiles())
            .filter(file -> file.isFile() && file.getName().endsWith(".csv"))
            .collect(Collectors.toList());

        if (datasets.isEmpty()) {
            throw new RuntimeException("No CSV datasets found in: " + dataDir.getAbsolutePath());
        }

        System.out.println("\nAvailable datasets:");
        for (int i = 0; i < datasets.size(); i++) {
            System.out.println((i + 1) + ". " + datasets.get(i).getName());
        }
        System.out.println("\nSelect a dataset (1-" + datasets.size() + "):");
        
        int choice = scanner.nextInt();
        if (choice < 1 || choice > datasets.size()) {
            throw new IllegalArgumentException("Invalid dataset choice");
        }

        return datasets.get(choice - 1).getAbsolutePath();
    }
    
    private String getModelName(Scanner scanner) {
        System.out.println("\nEnter a name for the trained model:");
        scanner.nextLine(); // Clear the buffer
        return scanner.nextLine();
    }
    
    public void startTraining() {
        try (Scanner scanner = new Scanner(System.in)) {
            String datasetPath = selectDataset(scanner);
            
            System.out.println("\nChoose model type to train:");
            System.out.println("1. Dense Neural Network (MLP)");
            System.out.println("2. Convolutional Neural Network (CNN)");
            
            int choice = scanner.nextInt();
            String modelName = getModelName(scanner);
            
            try {
                switch (choice) {
                    case 1:
                        System.out.println("Training Dense Neural Network...");
                        new DenseTraining().train(datasetPath, modelName);
                        break;
                    case 2:
                        System.out.println("Training CNN...");
                        new CnnTraining().train(datasetPath, modelName);
                        break;
                    default:
                        System.out.println("Invalid choice. Please select 1 or 2.");
                }
            } catch (IOException e) {
                System.err.println("Error during training: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new TrainerManager().startTraining();
    }
}

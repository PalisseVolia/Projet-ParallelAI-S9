package com.parallelai.exec.train;

import com.parallelai.training.CnnTraining;
import com.parallelai.training.DenseTraining;
import com.parallelai.database.FileDatabaseManager;

import org.nd4j.evaluation.regression.RegressionEvaluation;
import org.deeplearning4j.util.ModelSerializer;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
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
    
    private int getBatchSize(Scanner scanner) {
        System.out.println("\nEnter batch size (recommended: 32-128):");
        int batchSize = scanner.nextInt();
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        return batchSize;
    }
    
    private int getEpochs(Scanner scanner) {
        System.out.println("\nEnter number of epochs (recommended: 10-100):");
        int epochs = scanner.nextInt();
        if (epochs < 1) {
            throw new IllegalArgumentException("Number of epochs must be positive");
        }
        return epochs;
    }
    
    private void saveModelWithMetrics(String baseModelName, String modelType, TrainerResult result) throws IOException {
        DecimalFormat df = new DecimalFormat("0.000");
        RegressionEvaluation eval = result.getEvaluation();
        
        // Save model with metrics in the name (MSE, RMSE, R²)
        String metricsPrefix = String.format("%s_%s_%s_",
            df.format(eval.meanSquaredError(0)),
            df.format(eval.rootMeanSquaredError(0)),
            df.format(eval.rSquared(0)));
            
        String finalModelName = metricsPrefix + baseModelName;
        String modelPath = String.format("projet\\src\\main\\ressources\\models\\%s\\%s.zip", 
            modelType, finalModelName);
            
        // Print final metrics
        System.out.println("\nFinal Metrics:");
        System.out.println("MSE: " + eval.meanSquaredError(0));
        System.out.println("RMSE: " + eval.rootMeanSquaredError(0));
        System.out.println("R²: " + eval.rSquared(0));
        
        // Ensure directory exists
        new File(modelPath).getParentFile().mkdirs();
        
        // Save model locally
        ModelSerializer.writeModel(result.getModel(), modelPath, true);
        System.out.println("Model saved locally as: " + finalModelName);
        
        // Save model to database
        int modelTypeCode = modelType.equals("CNN") ? 1 : 2;
        FileDatabaseManager.insertFile(modelPath, modelTypeCode);

        // Delete local file after database insertion
        File localFile = new File(modelPath);
        localFile.delete();
    }
    
    public void startTraining() {
        try (Scanner scanner = new Scanner(System.in)) {
            String datasetPath = selectDataset(scanner);
            
            System.out.println("\nChoose model type to train:");
            System.out.println("1. Dense Neural Network (MLP)");
            System.out.println("2. Convolutional Neural Network (CNN)");
            
            int choice = scanner.nextInt();
            String modelName = getModelName(scanner);
            int batchSize = getBatchSize(scanner);
            int epochs = getEpochs(scanner);
            
            try {
                TrainerResult result;
                switch (choice) {
                    case 1:
                        System.out.println("Training Dense Neural Network...");
                        result = new DenseTraining().train(datasetPath, modelName, batchSize, epochs);
                        saveModelWithMetrics(modelName, "MLP", result);
                        break;
                    case 2:
                        System.out.println("Training CNN...");
                        result = new CnnTraining().train(datasetPath, modelName, batchSize, epochs);
                        saveModelWithMetrics(modelName, "CNN", result);
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

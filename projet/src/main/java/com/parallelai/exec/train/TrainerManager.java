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

/**
 * Gestionnaire d'entraînement des modèles.
 * Permet de :
 * - Sélectionner un jeu de données d'entraînement
 * - Choisir le type de modèle (Dense ou CNN)
 * - Configurer les paramètres d'entraînement
 * - Sauvegarder les modèles entraînés avec leurs métriques
 */
public class TrainerManager {
    // Chemin vers le répertoire contenant les jeux de données
    private static final String DATASET_DIR = "projet\\src\\main\\ressources\\data";
    
    /**
     * Permet à l'utilisateur de sélectionner un jeu de données parmi ceux disponibles.
     * 
     * @param scanner Scanner pour la saisie utilisateur
     * @return Le chemin absolu vers le fichier de données sélectionné
     * @throws RuntimeException Si le répertoire des datasets n'existe pas ou est vide
     * @throws IllegalArgumentException Si le choix de l'utilisateur est invalide
     */
    private String selectDataset(Scanner scanner) {
        File dataDir = new File(DATASET_DIR);
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            throw new RuntimeException("Répertoire de datasets non trouvé : " + dataDir.getAbsolutePath());
        }

        List<File> datasets = Arrays.stream(dataDir.listFiles())
            .filter(file -> file.isFile() && file.getName().endsWith(".csv"))
            .collect(Collectors.toList());

        if (datasets.isEmpty()) {
            throw new RuntimeException("Aucuns datasets trouvés: " + dataDir.getAbsolutePath());
        }

        System.out.println("\nJeux de données disponibles :");
        for (int i = 0; i < datasets.size(); i++) {
            System.out.println((i + 1) + ". " + datasets.get(i).getName());
        }
        System.out.println("\nSélectionnez un jeu de données (1-" + datasets.size() + ") :");
        
        int choice = scanner.nextInt();
        if (choice < 1 || choice > datasets.size()) {
            throw new IllegalArgumentException("Choix invalide");
        }

        return datasets.get(choice - 1).getAbsolutePath();
    }
    
    /**
     * Demande à l'utilisateur de saisir un nom pour le modèle.
     * 
     * @param scanner Scanner pour la saisie utilisateur
     * @return Le nom choisi pour le modèle
     */
    private String getModelName(Scanner scanner) {
        System.out.println("\nEntrez un nom pour le modèle à entrainer :");
        scanner.nextLine();
        return scanner.nextLine();
    }
    
    /**
     * Demande à l'utilisateur de définir la taille des batchs.
     * 
     * @param scanner Scanner pour la saisie utilisateur
     * @return La taille de batch choisie
     * @throws IllegalArgumentException Si la valeur saisie est négative
     */
    private int getBatchSize(Scanner scanner) {
        System.out.println("\nChoisissez la batch size (recommandé : 32-128) :");
        int batchSize = scanner.nextInt();
        if (batchSize < 1) {
            throw new IllegalArgumentException("La batch size doit être positif");
        }
        return batchSize;
    }
    
    /**
     * Demande à l'utilisateur de définir le nombre d'epochs.
     * 
     * @param scanner Scanner pour la saisie utilisateur
     * @return Le nombre d'epochs choisi
     * @throws IllegalArgumentException Si la valeur saisie est négative
     */
    private int getEpochs(Scanner scanner) {
        System.out.println("\nChoisissez le nombre d'epochs (recommandé: 10-100):");
        int epochs = scanner.nextInt();
        if (epochs < 1) {
            throw new IllegalArgumentException("Le nombre d'epochs doit être positif");
        }
        return epochs;
    }
    
    /**
     * Sauvegarde le modèle entraîné avec ses métriques de performance.
     * Le nom du fichier inclut les métriques principales (MSE, RMSE, R²).
     * 
     * @param baseModelName Nom de base du modèle
     * @param modelType Type du modèle (CNN ou MLP)
     * @param result Résultats de l'entraînement
     * @throws IOException En cas d'erreur lors de la sauvegarde
     */
    private void saveModelWithMetrics(String baseModelName, String modelType, TrainerResult result) throws IOException {
        DecimalFormat df = new DecimalFormat("0.000");
        RegressionEvaluation eval = result.getEvaluation();
        
        // Sauvegarde du modèle avec les métriques dans le nom (MSE, RMSE, R²)
        String metricsPrefix = String.format("%s_%s_%s_",
            df.format(eval.meanSquaredError(0)),
            df.format(eval.rootMeanSquaredError(0)),
            df.format(eval.rSquared(0)));
            
        String finalModelName = metricsPrefix + baseModelName;
        String modelPath = String.format("projet\\src\\main\\ressources\\models\\%s\\%s.zip", 
            modelType, finalModelName);
            
        // Métriques finales
        System.out.println("\nIndicateurs finaux :");
        System.out.println("MSE : " + eval.meanSquaredError(0));
        System.out.println("RMSE : " + eval.rootMeanSquaredError(0));
        System.out.println("R² : " + eval.rSquared(0));
        
        // Création du répertoire si nécessaire
        new File(modelPath).getParentFile().mkdirs();
        
        // Sauvegarde locale du modèle
        ModelSerializer.writeModel(result.getModel(), modelPath, true);
        System.out.println("Modèle sauvegardé localement sous : " + finalModelName);
        
        // Sauvegarde du modèle dans la base de données
        int modelTypeCode = modelType.equals("CNN") ? 1 : 2;
        FileDatabaseManager.insertFile(modelPath, modelTypeCode);

        // Suppression du fichier local après insertion dans la base
        File localFile = new File(modelPath);
        localFile.delete();
    }
    
    /**
     * Lance le processus d'entraînement interactif.
     * Guide l'utilisateur à travers les différentes étapes de configuration
     * et d'entraînement du modèle choisi.
     */
    public void startTraining() {
        try (Scanner scanner = new Scanner(System.in)) {
            String datasetPath = selectDataset(scanner);
            
            System.out.println("\nChoisissez le type de modèle à entraîner :");
            System.out.println("1. Réseau de neurones dense (MLP)");
            System.out.println("2. Réseau de neurones convolutif (CNN)");
            
            int choice = scanner.nextInt();
            String modelName = getModelName(scanner);
            int batchSize = getBatchSize(scanner);
            int epochs = getEpochs(scanner);
            
            try {
                TrainerResult result;
                switch (choice) {
                    case 1:
                        System.out.println("Entraînement du réseau de neurones dense...");
                        result = new DenseTraining().train(datasetPath, modelName, batchSize, epochs);
                        saveModelWithMetrics(modelName, "MLP", result);
                        break;
                    case 2:
                        System.out.println("Entraînement du CNN...");
                        result = new CnnTraining().train(datasetPath, modelName, batchSize, epochs);
                        saveModelWithMetrics(modelName, "CNN", result);
                        break;
                    default:
                        System.out.println("Choix invalide. Veuillez sélectionner 1 ou 2.");
                }
            } catch (IOException e) {
                System.err.println("Erreur pendant l'entraînement : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Point d'entrée principal du gestionnaire d'entraînement pour une exécution rapide.
     * 
     * @param args Arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        new TrainerManager().startTraining();
    }
}

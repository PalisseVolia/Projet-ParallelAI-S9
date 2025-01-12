package com.parallelai.models.utils;

import java.util.ArrayList;
import java.util.List;

import com.parallelai.models.RandomModel;
import com.parallelai.models.CnnModel;
import com.parallelai.models.DenseModel;
import com.parallelai.database.FileDatabaseManager;
import java.io.File;

/**
 * Registre pour gérer et fournir l'accès aux modèles d'IA.
 * Cette classe maintient une liste des modèles d'IA disponibles et fournit des méthodes
 * pour enregistrer de nouveaux modèles et créer des instances des modèles existants.
 */
public class ModelRegistry {
    /** Liste de tous les modèles d'IA enregistrés */
    private static final List<ModelInfo> availableModels = new ArrayList<>();
    private static String modelName;

    static {
        // Enregistrement de tous les modèles disponibles
        registerModel("Random", () -> new RandomModel());
        registerModel("CNN", () -> new CnnModel(modelName));
        registerModel("Dense", () -> new DenseModel(modelName));
    }

    /**
     * Enregistre un nouveau modèle d'IA dans le registre
     * @param name Le nom d'affichage du modèle
     * @param supplier Une fonction qui crée une nouvelle instance du modèle
     */
    public static void registerModel(String name, ModelSupplier supplier) {
        availableModels.add(new ModelInfo(name, supplier));
    }

    /**
     * Retourne la liste de tous les modèles d'IA disponibles
     * @return Liste des modèles d'IA enregistrés
     */
    public static List<ModelInfo> getAvailableModels() {
        return new ArrayList<>(availableModels);
    }

    /**
     * Crée une nouvelle instance d'un modèle à l'index spécifié
     * @param index L'index du modèle dans le registre
     * @return Une nouvelle instance du modèle demandé
     */
    public static Model createModel(int index, String modelName) {
        ModelRegistry.modelName = modelName;
        return availableModels.get(index).supplier.get();
    }

    /**
     * Interface fonctionnelle pour créer de nouvelles instances de modèles
     */
    @FunctionalInterface
    public interface ModelSupplier {
        Model get();
    }

    /**
     * Classe contenant les informations des modèles.
     */
    public static class ModelInfo {
        /** Nom du modèle */
        public final String name;
        /** Fonction pour créer de nouvelles instances du modèle */
        public final ModelSupplier supplier;

        /**
         * Crée un nouveau ModelInfo
         * @param name Nom d'affichage du modèle
         * @param supplier Fonction pour créer des instances du modèle
         */
        ModelInfo(String name, ModelSupplier supplier) {
            this.name = name;
            this.supplier = supplier;
        }
    }

    /**
     * Initialise un modèle depuis la base de données
     * @param modelType Le type de modèle à initialiser (CNN ou MLP)
     */
    public static void initializeModelFromDatabase(String modelType) {
        int dbType = modelType.equals("CNN") ? 1 : 2;
        String[] availableModels = FileDatabaseManager.getFileList(dbType);
        
        if (availableModels.length == 0) {
            throw new RuntimeException("Aucun modèle " + modelType + " disponible dans la base de données");
        }

        // Utilise le premier modèle par défaut
        String selectedModel = availableModels[0];
        String targetPath = String.format("projet\\src\\main\\ressources\\models\\%s\\othello_%s_model.zip",
                modelType, modelType.toLowerCase());

        // Assure que le répertoire cible existe
        new File(targetPath).getParentFile().mkdirs();

        // Télécharge le modèle sélectionné
        FileDatabaseManager.downloadFile(selectedModel, dbType);
    }

    /**
     * Initialise un modèle spécifique depuis la base de données
     * @param modelType Le type de modèle à initialiser (CNN ou MLP)
     * @param modelName Le nom du modèle à télécharger
     */
    public static void initializeModelFromDatabase(String modelType, String modelName) {
        int dbType = modelType.equals("CNN") ? 1 : 2;
        String[] availableModels = FileDatabaseManager.getFileList(dbType);
        
        // Vérifie si le modèle demandé existe
        boolean modelExists = false;
        for (String model : availableModels) {
            if (model.equals(modelName)) {
                modelExists = true;
                break;
            }
        }
        
        if (!modelExists) {
            throw new RuntimeException("Le modèle " + modelName + " n'est pas disponible dans la base de données");
        }

        String targetPath = String.format("projet\\src\\main\\ressources\\models\\%s\\%s",
                modelType, modelName);

        // Assure que le répertoire cible existe
        new File(targetPath).getParentFile().mkdirs();

        // Télécharge le modèle sélectionné
        FileDatabaseManager.downloadFile(modelName, dbType);
    }
}
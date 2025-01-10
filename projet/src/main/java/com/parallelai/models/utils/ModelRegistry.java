package com.parallelai.models.utils;

import java.util.ArrayList;
import java.util.List;

import com.parallelai.models.RandomModel;
import com.parallelai.models.CnnModel;
import com.parallelai.models.DenseModel;
import com.parallelai.database.FileDatabaseManager;
import java.io.File;

/**
 * Registry for managing and providing access to AI models.
 * This class maintains a list of available AI models and provides methods
 * to register new models and create instances of existing ones.
 */
public class ModelRegistry {
    /** List of all registered AI models */
    private static final List<ModelInfo> availableModels = new ArrayList<>();

    static {
        // Register all available models here
        registerModel("Random", () -> new RandomModel());
        registerModel("CNN", () -> new CnnModel());
        registerModel("Dense", () -> new DenseModel());  // Add this line
        // Add more models as they become available
    }

    /**
     * Registers a new AI model in the registry
     * @param name The display name of the model
     * @param supplier A function that creates a new instance of the model
     */
    public static void registerModel(String name, ModelSupplier supplier) {
        availableModels.add(new ModelInfo(name, supplier));
    }

    /**
     * Returns a list of all available AI models
     * @return List of ModelInfo objects containing model names and their factories
     */
    public static List<ModelInfo> getAvailableModels() {
        return new ArrayList<>(availableModels);
    }

    /**
     * Creates a new instance of a model at the specified index
     * @param index The index of the model in the registry
     * @return A new instance of the requested model
     */
    public static Model createModel(int index) {
        return availableModels.get(index).supplier.get();
    }

    /**
     * Functional interface for creating new model instances
     */
    @FunctionalInterface
    public interface ModelSupplier {
        Model get();
    }

    /**
     * Container class for model information
     * Holds the display name and instance factory for a model
     */
    public static class ModelInfo {
        /** Display name of the model */
        public final String name;
        /** Factory function to create new instances of the model */
        public final ModelSupplier supplier;

        /**
         * Creates a new ModelInfo
         * @param name Display name of the model
         * @param supplier Factory function for creating model instances
         */
        ModelInfo(String name, ModelSupplier supplier) {
            this.name = name;
            this.supplier = supplier;
        }
    }

    public static void initializeModelFromDatabase(String modelType) {
        int dbType = modelType.equals("CNN") ? 1 : 2;
        String[] availableModels = FileDatabaseManager.getFileList(dbType);
        
        if (availableModels.length == 0) {
            throw new RuntimeException("No " + modelType + " models available in the database");
        }

        String selectedModel = availableModels[0]; // Default to first model
        String targetPath = String.format("projet\\src\\main\\ressources\\models\\%s\\othello_%s_model.zip",
                modelType, modelType.toLowerCase());

        // Ensure the target directory exists
        new File(targetPath).getParentFile().mkdirs();

        // Download the selected model
        FileDatabaseManager.downloadFile(selectedModel, dbType);
    }
}
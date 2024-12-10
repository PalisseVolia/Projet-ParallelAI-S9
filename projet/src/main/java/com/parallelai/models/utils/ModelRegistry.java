package com.parallelai.models.utils;

import java.util.ArrayList;
import java.util.List;

import com.parallelai.models.MinimaxModel;
import com.parallelai.models.RandomModel;

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
        registerModel("Minimax", () -> new MinimaxModel());
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
}
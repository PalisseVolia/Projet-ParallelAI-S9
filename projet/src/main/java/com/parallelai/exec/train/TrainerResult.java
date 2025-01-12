package com.parallelai.exec.train;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.evaluation.regression.RegressionEvaluation;

/**
 * Classe contenant les résultats de l'entraînement d'un modèle d'IA.
 */
public class TrainerResult {
    private final MultiLayerNetwork model;
    private final RegressionEvaluation evaluation;

    /**
     * Constructeur pour les résultats d'entraînement.
     * 
     * @param model      Le modèle entraîné
     * @param evaluation L'évaluation des performances du modèle
     */
    public TrainerResult(MultiLayerNetwork model, RegressionEvaluation evaluation) {
        this.model = model;
        this.evaluation = evaluation;
    }

    /**
     * @return Le modèle entraîné
     */
    public MultiLayerNetwork getModel() {
        return model;
    }

    /**
     * @return L'évaluation des performances du modèle
     */
    public RegressionEvaluation getEvaluation() {
        return evaluation;
    }
}

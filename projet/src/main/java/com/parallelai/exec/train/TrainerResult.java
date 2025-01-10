package com.parallelai.exec.train;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.evaluation.regression.RegressionEvaluation;

public class TrainerResult {
    private final MultiLayerNetwork model;
    private final RegressionEvaluation evaluation;

    public TrainerResult(MultiLayerNetwork model, RegressionEvaluation evaluation) {
        this.model = model;
        this.evaluation = evaluation;
    }

    public MultiLayerNetwork getModel() {
        return model;
    }

    public RegressionEvaluation getEvaluation() {
        return evaluation;
    }
}

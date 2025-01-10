package com.parallelai.training;

import com.parallelai.exec.train.TrainerMetrics;
import com.parallelai.exec.train.TrainerResult;
import com.parallelai.training.utils.DatasetImporter;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.regression.RegressionEvaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.dataset.DataSet;

import java.io.*;

public class CnnTraining {
    private static final int BOARD_SIZE = 8;
    
    public TrainerResult train(String datasetPath, String modelName, int batchSize, int nEpochs) throws IOException {
        // Load and prepare data
        DatasetImporter importer = new DatasetImporter();
        DataSetIterator trainIterator = importer.importDataset(datasetPath, batchSize);
        // Create a separate iterator for evaluation
        DataSetIterator evalIterator = importer.importDataset(datasetPath, batchSize);
        
        // Configure network
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(0.001))
            .list()
            .layer(new ConvolutionLayer.Builder()
                .kernelSize(3,3)
                .stride(1,1)
                .nIn(1)
                .nOut(32)
                .activation(Activation.RELU)
                .build())
            .layer(new ConvolutionLayer.Builder()
                .kernelSize(3,3)
                .stride(1,1)
                .nOut(64)
                .activation(Activation.RELU)
                .build())
            .layer(new DenseLayer.Builder()
                .nOut(128)
                .activation(Activation.RELU)
                .build())
            .layer(new OutputLayer.Builder()
                .nOut(1)
                .activation(Activation.IDENTITY)  // Using IDENTITY for regression
                .lossFunction(org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction.MSE)
                .build())
            .setInputType(InputType.convolutional(BOARD_SIZE, BOARD_SIZE, 1))
            .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        // Add listeners for metrics
        model.setListeners(new TrainerMetrics(nEpochs), new ScoreIterationListener(10));

        // Train model with evaluation after each epoch
        System.out.println("Starting training...");
        RegressionEvaluation finalEval = null;
        for (int i = 0; i < nEpochs; i++) {
            model.fit(trainIterator);
            
            // Evaluate model
            RegressionEvaluation eval = new RegressionEvaluation(1);  // 1 output column
            
            // Evaluate on the evaluation dataset
            while (evalIterator.hasNext()) {
                DataSet ds = evalIterator.next();
                eval.eval(ds.getLabels(), model.output(ds.getFeatures()));
            }
            
            // Print metrics
            System.out.println(String.format("Epoch %d/%d", (i + 1), nEpochs));
            System.out.println("MSE: " + eval.meanSquaredError(0));
            System.out.println("RMSE: " + eval.rootMeanSquaredError(0));
            System.out.println("R²: " + eval.rSquared(0));
            System.out.println("--------------------");
            
            // Reset iterators
            trainIterator.reset();
            evalIterator.reset();
            
            finalEval = eval;
        }

        // Return both model and evaluation
        return new TrainerResult(model, finalEval);
    }
}
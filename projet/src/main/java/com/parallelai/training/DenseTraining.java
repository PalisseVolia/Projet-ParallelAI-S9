package com.parallelai.training;

import com.parallelai.exec.train.TrainerMetrics;
import com.parallelai.exec.train.TrainerResult;
import com.parallelai.training.utils.DatasetImporter;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.evaluation.regression.RegressionEvaluation;
import org.nd4j.linalg.dataset.DataSet;
import org.deeplearning4j.util.ModelSerializer;

import java.io.*;

public class DenseTraining {
    private static final int BOARD_SIZE = 8;
    private static final int INPUT_SIZE = BOARD_SIZE * BOARD_SIZE;
    
    public TrainerResult train(String datasetPath, String modelName, int batchSize, int nEpochs) throws IOException {
        // Load and prepare data
        DatasetImporter importer = new DatasetImporter();
        // Split dataset into training (80%) and validation (20%) sets
        DataSetIterator fullIterator = importer.importDataset(datasetPath, batchSize);
        
        // Calculate total samples and split sizes
        int totalSamples = 0;
        while (fullIterator.hasNext()) {
            fullIterator.next();
            totalSamples += batchSize;
        }
        fullIterator.reset();
        
        int trainSize = (int) (totalSamples * 0.8);
        int evalSize = totalSamples - trainSize;
        
        DataSetIterator[] iterators = importer.splitDataset(datasetPath, batchSize, trainSize, evalSize);
        DataSetIterator trainIterator = iterators[0];
        DataSetIterator evalIterator = iterators[1];
        
        // Configure network
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(0.001))
            .list()
            .layer(new DenseLayer.Builder()
                .nIn(INPUT_SIZE)
                .nOut(256)
                .activation(Activation.RELU)
                .build())
            .layer(new DenseLayer.Builder()
                .nOut(128)
                .activation(Activation.RELU)
                .build())
            .layer(new DenseLayer.Builder()
                .nOut(64)
                .activation(Activation.RELU)
                .build())
            .layer(new OutputLayer.Builder()
                .nOut(1)
                .activation(Activation.SIGMOID)
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
        double bestMSE = Double.MAX_VALUE;
        MultiLayerNetwork bestModel = null;
        
        for (int i = 0; i < nEpochs; i++) {
            model.fit(trainIterator);
            
            // Evaluate model
            RegressionEvaluation eval = new RegressionEvaluation(1);
            
            while (evalIterator.hasNext()) {
                DataSet ds = evalIterator.next();
                eval.eval(ds.getLabels(), model.output(ds.getFeatures()));
            }
            
            // Check if current model is better
            double currentMSE = eval.meanSquaredError(0);
            if (currentMSE < bestMSE) {
                bestMSE = currentMSE;
                // Save the best model
                File tempFile = File.createTempFile("bestmodel", "tmp");
                ModelSerializer.writeModel(model, tempFile, true);
                bestModel = ModelSerializer.restoreMultiLayerNetwork(tempFile);
                tempFile.delete();
                finalEval = eval;
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
        }

        // Return best model and its evaluation
        return new TrainerResult(bestModel, finalEval);
    }
}

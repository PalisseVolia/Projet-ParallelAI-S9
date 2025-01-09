package com.parallelai.training;

import com.parallelai.exec.train.TrainerMetrics;
import com.parallelai.training.utils.DatasetImporter;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Adam;

import java.io.*;

public class DenseTraining {
    private static final int BOARD_SIZE = 8;
    private static final int INPUT_SIZE = BOARD_SIZE * BOARD_SIZE;
    
    public void train(String datasetPath, String modelName, int batchSize, int nEpochs) throws IOException {
        // Load and prepare data
        DatasetImporter importer = new DatasetImporter();
        DataSetIterator iterator = importer.importDataset(datasetPath, batchSize);
        
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
                .lossFunction(org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction.XENT)
                .build())
            .setInputType(InputType.convolutional(BOARD_SIZE, BOARD_SIZE, 1))
            .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        // Add listeners for metrics
        model.setListeners(new TrainerMetrics(nEpochs), new ScoreIterationListener(10));

        // Train model
        System.out.println("Starting training...");
        for (int i = 0; i < nEpochs; i++) {
            model.fit(iterator);
            iterator.reset();
            System.out.println("Completed epoch " + (i + 1) + "/" + nEpochs);
        }

        // Save model with custom name
        String modelPath = String.format("projet\\src\\main\\ressources\\models\\MLP\\%s.zip", modelName);
        ModelSerializer.writeModel(model, modelPath, true);
    }
}

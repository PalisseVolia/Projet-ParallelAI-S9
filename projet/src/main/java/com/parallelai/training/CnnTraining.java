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
import org.deeplearning4j.util.ModelSerializer;

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
                .layer(new DropoutLayer(0.8))
            .layer(new ConvolutionLayer.Builder()
                .kernelSize(3,3)
                .stride(1,1)
                .nOut(64)
                .activation(Activation.RELU)
                .build())
                .layer(new DropoutLayer(0.6))
            .layer(new DenseLayer.Builder()
                .nOut(128)
                .activation(Activation.RELU)
                .build())
            .layer(new OutputLayer.Builder()
                .nOut(1)
                .activation(Activation.IDENTITY)
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
            RegressionEvaluation eval = new RegressionEvaluation(1);  // 1 output column
            
            // Evaluate on the evaluation dataset
            while (evalIterator.hasNext()) {
                DataSet ds = evalIterator.next();
                eval.eval(ds.getLabels(), model.output(ds.getFeatures()));
            }
            
            // Si le modèle actuel est meilleur que le meilleur modèle précédent
            double currentMSE = eval.meanSquaredError(0);
            if (currentMSE < bestMSE) {
                bestMSE = currentMSE;
                // Sauvegarder le meilleur modèle
                File tempFile = File.createTempFile("bestmodel", "tmp");
                ModelSerializer.writeModel(model, tempFile, true);
                bestModel = ModelSerializer.restoreMultiLayerNetwork(tempFile);
                tempFile.delete();
                finalEval = eval;
            }
            
            // Afficher les métriques
            System.out.println(String.format("Epoch %d/%d", (i + 1), nEpochs));
            System.out.println("MSE: " + eval.meanSquaredError(0));
            System.out.println("RMSE: " + eval.rootMeanSquaredError(0));
            System.out.println("R²: " + eval.rSquared(0));
            System.out.println("--------------------");
            
            // Reset iterators
            trainIterator.reset();
            evalIterator.reset();
        }

        // Return le meilleur modèle et ses métriques finales
        return new TrainerResult(bestModel, finalEval);
    }
}
package com.projet.training;

import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OthelloCNN {
    private static final int BOARD_SIZE = 8;
    private static final int BATCH_SIZE = 64;
    private static final int N_EPOCHS = 10;
    private MultiLayerNetwork model;

    public void initializeNetwork() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(0.001))
            .l2(0.0001)
            .list()
            .layer(0, new ConvolutionLayer.Builder()
                .kernelSize(3, 3)
                .stride(1, 1)
                .nIn(1)
                .nOut(32)
                .activation(Activation.RELU)
                .build())
            .layer(1, new SubsamplingLayer.Builder()
                .kernelSize(2, 2)
                .stride(1, 1)
                .poolingType(SubsamplingLayer.PoolingType.MAX)
                .build())
            .layer(2, new ConvolutionLayer.Builder()
                .kernelSize(2, 2)
                .stride(1, 1)
                .nOut(64)
                .activation(Activation.RELU)
                .build())
            .layer(3, new DenseLayer.Builder()
                .nOut(128)
                .activation(Activation.RELU)
                .build())
            .layer(4, new OutputLayer.Builder()
                .nOut(1)
                .activation(Activation.SIGMOID)
                .lossFunction(LossFunctions.LossFunction.MSE)
                .build())
            .setInputType(InputType.convolutional(BOARD_SIZE, BOARD_SIZE, 1))
            .build();

        model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(new ScoreIterationListener(100));
    }

    public DataSetIterator loadData(String filename) throws IOException {
        List<DataSet> dataSets = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                
                // Create input array (8x8 board)
                INDArray input = Nd4j.zeros(1, 1, BOARD_SIZE, BOARD_SIZE);
                for (int i = 0; i < 64; i++) {
                    int row = i / BOARD_SIZE;
                    int col = i % BOARD_SIZE;
                    input.putScalar(0, 0, row, col, Double.parseDouble(values[i]));
                }
                
                // Create output array (game outcome)
                INDArray output = Nd4j.zeros(1, 1);
                output.putScalar(0, Double.parseDouble(values[64]));
                
                dataSets.add(new DataSet(input, output));
            }
        }
        
        Collections.shuffle(dataSets);
        return new ListDataSetIterator<>(dataSets, BATCH_SIZE);
    }

    public void train(DataSetIterator trainData) {
        for (int i = 0; i < N_EPOCHS; i++) {
            System.out.println("Starting epoch " + (i + 1));
            trainData.reset();
            while (trainData.hasNext()) {
                DataSet batch = trainData.next();
                model.fit(batch);
            }
        }
    }

    public double predict(double[][] board) {
        INDArray input = Nd4j.zeros(1, 1, BOARD_SIZE, BOARD_SIZE);
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                input.putScalar(0, 0, i, j, board[i][j]);
            }
        }
        INDArray output = model.output(input);
        return output.getDouble(0);
    }

    public void saveModel(String filepath) throws IOException {
        model.save(new File(filepath));
    }

    public void loadModel(String filepath) throws IOException {
        model = MultiLayerNetwork.load(new File(filepath), true);
    }

    public static void main(String[] args) throws IOException {
        OthelloCNN cnn = new OthelloCNN();
        cnn.initializeNetwork();

        // Load both black and white positions
        DataSetIterator blackData = cnn.loadData("projet\\src\\main\\java\\com\\projet\\training\\temp\\noirs.csv");
        // DataSetIterator whiteData = cnn.loadData("projet\\src\\main\\java\\com\\projet\\training\\temp\\blancs.csv");
        
        // Train on black positions
        System.out.println("Training on black positions...");
        cnn.train(blackData);
        
        // Train on white positions
        // System.out.println("Training on white positions...");
        // cnn.train(whiteData);
        
        // Save the trained model
        cnn.saveModel("projet\\src\\main\\java\\com\\projet\\training\\models\\othello_model.zip");
    }
}
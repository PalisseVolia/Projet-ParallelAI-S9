package com.parallelai.models;

import com.parallelai.game.Board;
import com.parallelai.game.Move;
import com.parallelai.game.Disc;
import com.parallelai.models.utils.Model;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;

public class DenseModel implements Model {
    private MultiLayerNetwork network;
    private static final int BOARD_SIZE = 8;
    private static final int INPUT_SIZE = BOARD_SIZE * BOARD_SIZE;

    public DenseModel() {
        try {
            // Load the trained model
            String modelPath = "projet\\src\\main\\java\\com\\parallelai\\training\\models\\othello_dense_model.zip";
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                throw new IOException("Model file not found at: " + modelFile.getAbsolutePath());
            }
            this.network = ModelSerializer.restoreMultiLayerNetwork(modelFile);
        } catch (IOException e) {
            System.err.println("Error loading Dense model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public double evaluateMove(Move move, Board board) {
        if (network == null) {
            return 0.5; // Return neutral score if model failed to load
        }

        try {
            // Create a copy of the board and apply the move
            Board boardCopy = board.copy();
            boardCopy.makeMove(move);

            // Convert board state to input format expected by Dense network
            INDArray input = boardToINDArray(boardCopy);
            
            // Get model prediction
            INDArray output = network.output(input);
            return output.getDouble(0);
        } catch (Exception e) {
            System.err.println("Error during move evaluation: " + e.getMessage());
            return 0.5; // Return neutral score on error
        }
    }

    private INDArray boardToINDArray(Board board) {
        // Create a 2D array with shape [1, 64] (batch size, flattened board)
        INDArray input = Nd4j.zeros(1, INPUT_SIZE);
        
        Disc[][] grid = board.getGrid();
        // Fill the array with flattened board state
        int idx = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                // Convert Disc enum to numerical value
                // Empty = 0, Black = 1, White = -1
                float value = 0;
                if (grid[i][j] == Disc.BLACK) {
                    value = 1;
                } else if (grid[i][j] == Disc.WHITE) {
                    value = -1;
                }
                input.putScalar(new int[]{0, idx}, value);
                idx++;
            }
        }
        
        return input;
    }
}

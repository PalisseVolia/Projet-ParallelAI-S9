package com.parallelai.models;

import com.parallelai.game.Board;
import com.parallelai.game.Move;
import com.parallelai.game.Disc;
import com.parallelai.models.utils.Model;
import com.parallelai.models.utils.ModelRegistry;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;

/**
 * Modèle utilisant un réseau de neurones dense (MLP) pour évaluer les coups.
 */
public class DenseModel implements Model {
    private MultiLayerNetwork network;
    private static final int BOARD_SIZE = 8;
    private static final int INPUT_SIZE = BOARD_SIZE * BOARD_SIZE;
    private static final String MODEL_PATH = "projet\\src\\main\\ressources\\models\\MLP\\";

    /**
     * Initialise le modèle dense en chargeant le réseau de neurones pré-entraîné.
     */
    public DenseModel(String modelName) {
        try {
            ModelRegistry.initializeModelFromDatabase("MLP", modelName);
            File modelFile = new File(MODEL_PATH+modelName);
            if (!modelFile.exists()) {
                throw new IOException("Fichier modèle non trouvé à l'emplacement : " + modelFile.getAbsolutePath());
            }
            this.network = ModelSerializer.restoreMultiLayerNetwork(modelFile);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du modèle : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Évalue un coup en utilisant le réseau de neurones dense.
     *
     * @param move Le coup à évaluer
     * @param board L'état actuel du plateau
     * @return Une valeur entre 0 et 1 représentant la qualité estimée du coup
     */
    @Override
    public double evaluateMove(Move move, Board board) {
        if (network == null) {
            return 0.5; // Retourne un score neutre si le modèle n'est pas chargé
        }

        try {
            // Crée une copie du plateau et applique le coup
            Board boardCopy = board.copy();
            boardCopy.makeMove(move);

            // Convertit l'état du plateau au format attendu par le MLP
            INDArray input = boardToINDArray(boardCopy);
            
            // Obtient la prédiction du modèle
            INDArray output = network.output(input);
            return output.getDouble(0);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'évaluation du coup : " + e.getMessage());
            return 0.5; // Retourne un score neutre en cas d'erreur
        }
    }

    /**
     * Convertit l'état du plateau en un format approprié pour le réseau dense.
     *
     * @param board Le plateau à convertir
     * @return Une représentation du plateau sous forme de tableau multidimensionnel
     */
    private INDArray boardToINDArray(Board board) {
        // Crée un tableau 2D de forme [1, 64] pour représenter l'état du plateau
        INDArray input = Nd4j.zeros(1, INPUT_SIZE);
        
        Disc[][] grid = board.getGrid();
        // Remplit le tableau avec l'état du plateau
        int idx = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                // Convertit l'énumération Disc en valeur numérique
                // Vide = 0, Noir = 1, Blanc = -1
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

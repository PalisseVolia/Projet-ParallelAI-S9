package com.parallelai.training.utils;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classe utilitaire pour importer des données d'entraînement à partir de fichiers CSV.
 * Cette classe est spécialement conçue pour traiter un plateau de 8x8.
 */
public class DatasetImporter {
    private static final int BOARD_SIZE = 8;

    /**
     * Importe un dataset à partir d'un fichier CSV et le convertit en DataSetIterator.
     *
     * @param filepath  Le chemin du fichier CSV contenant les données
     * @param batchSize Le batchsize pour l'entraînement
     * @return Un DataSetIterator contenant les données formatées pour l'entraînement
     * @throws IOException Si une erreur survient lors de la lecture du fichier
     */
    public DataSetIterator importDataset(String filepath, int batchSize) throws IOException {
        List<DataSet> dataset = loadDataFromCsv(filepath);
        return new ListDataSetIterator<>(dataset, batchSize);
    }

    /**
     * Charge les données depuis un fichier CSV et les convertit en DataSet compatible.
     * Le fichier doit contenir 65 colonnes : 64 pour la grille 8x8 et 1 pour la valeur cible.
     *
     * @param filepath Le chemin du fichier CSV à charger
     * @return Une liste de DataSet contenant les données chargées et mélangées
     * @throws IOException Si une erreur survient lors de la lecture du fichier
     */
    private List<DataSet> loadDataFromCsv(String filepath) throws IOException {
        List<DataSet> dataset = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            // Ignorer le header si il existe
            br.readLine();
            
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                
                // Créer un tableau d'input (8x8x1)
                INDArray input = Nd4j.zeros(1, 1, BOARD_SIZE, BOARD_SIZE);
                for (int i = 0; i < 64; i++) {
                    int row = i / BOARD_SIZE;
                    int col = i % BOARD_SIZE;
                    input.putScalar(new int[]{0, 0, row, col}, Double.parseDouble(values[i]));
                }
                
                // Créer un scalaire d'output (une seule valeur entre 0-1)
                INDArray output = Nd4j.zeros(1, 1);
                output.putScalar(0, Double.parseDouble(values[64]));
                
                dataset.add(new DataSet(input, output));
            }
        }
        
        Collections.shuffle(dataset);
        return dataset;
    }

    public DataSetIterator[] splitDataset(String datasetPath, int batchSize, int trainSize, int evalSize) {
        // Load full dataset
        DataSetIterator fullIterator;
        try {
            fullIterator = importDataset(datasetPath, batchSize);
        } catch (IOException e) {
            e.printStackTrace();
            return new DataSetIterator[0]; // Return an empty array in case of error
        }
        
        // Create lists to store split data
        List<DataSet> trainData = new ArrayList<>();
        List<DataSet> evalData = new ArrayList<>();
        
        int currentSize = 0;
        while (fullIterator.hasNext() && currentSize < trainSize) {
            trainData.add(fullIterator.next());
            currentSize += batchSize;
        }
        
        while (fullIterator.hasNext() && currentSize < (trainSize + evalSize)) {
            evalData.add(fullIterator.next());
            currentSize += batchSize;
        }
        
        // Create iterators from the split data
        DataSetIterator trainIterator = new ListDataSetIterator<>(trainData, batchSize);
        DataSetIterator evalIterator = new ListDataSetIterator<>(evalData, batchSize);
        
        return new DataSetIterator[] {trainIterator, evalIterator};
    }
}

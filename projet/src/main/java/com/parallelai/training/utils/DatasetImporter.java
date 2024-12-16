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

public class DatasetImporter {
    private static final int BOARD_SIZE = 8;

    public DataSetIterator importDataset(String filepath, int batchSize) throws IOException {
        List<DataSet> dataset = loadDataFromCsv(filepath);
        return new ListDataSetIterator<>(dataset, batchSize);
    }

    private List<DataSet> loadDataFromCsv(String filepath) throws IOException {
        List<DataSet> dataset = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            br.readLine(); // Skip header if exists
            
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                
                // Create input array (8x8x1)
                INDArray input = Nd4j.zeros(1, 1, BOARD_SIZE, BOARD_SIZE);
                for (int i = 0; i < 64; i++) {
                    int row = i / BOARD_SIZE;
                    int col = i % BOARD_SIZE;
                    input.putScalar(new int[]{0, 0, row, col}, Double.parseDouble(values[i]));
                }
                
                // Create output (single value between 0-1)
                INDArray output = Nd4j.zeros(1, 1);
                output.putScalar(0, Double.parseDouble(values[65]));
                
                dataset.add(new DataSet(input, output));
            }
        }
        
        Collections.shuffle(dataset);
        return dataset;
    }
}

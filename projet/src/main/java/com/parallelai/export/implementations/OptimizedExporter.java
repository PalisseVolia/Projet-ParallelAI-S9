package com.parallelai.export.implementations;

import com.parallelai.export.GameStateExporter;
import com.parallelai.export.utilities.GameExporterUtils.*;
import com.parallelai.models.utils.Model;
import java.util.*;

public class OptimizedExporter extends GameStateExporter {
    
    public OptimizedExporter(String outputPath) {
        super(outputPath);
    }

    // Copier startGamesWithUniqueStatesOptimized depuis GameStateExporter
    public void startGamesWithUniqueStatesOptimized(int nbParties, Model model1, Model model2, int nbThreads) {
    // Chaque thread utilise sa propre HashMap locale
    @SuppressWarnings("unchecked")
    Map<String, double[]>[] threadMaps = new HashMap[nbThreads];
    for (int i = 0; i < nbThreads; i++) {
        threadMaps[i] = new HashMap<>();
    }

    // Les threads travaillent sur leurs maps locales sans synchronisation

    // Une seule fusion à la fin
    Map<String, double[]> finalMap = new HashMap<>();
    for (Map<String, double[]> threadMap : threadMaps) {
        threadMap.forEach((key, value) -> {
            finalMap.merge(key, value, (existing, newVal) -> {
                existing[64] += newVal[64];
                existing[65] += newVal[65];
                return existing;
            });
        });
    }
}
}

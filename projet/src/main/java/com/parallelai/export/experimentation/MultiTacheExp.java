package com.parallelai.export.experimentation;

import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import com.parallelai.export.GameStateExporter;
import com.parallelai.models.RandomModel;
import com.parallelai.models.utils.Model;

public class MultiTacheExp {
    private static final int NB_PARTIES = 20000; // Nombre de parties par test
    private static final int NB_TESTS = 2; // Nombre de tests par configuration
    private static final String RESULTS_PATH = String.format("projet/src/main/ressources/evaldata_multitache/threading_performance_%dparties.csv", NB_PARTIES);

    public static void main(String[] args) {
        File directory = new File("projet/src/main/ressources/evaldata_multitache");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Créer l'en-tête du fichier CSV
        try (FileWriter writer = new FileWriter(RESULTS_PATH)) {
            writer.write("nb_threads,nb_parties,execution_time_ms_avg\n");
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier CSV: " + e.getMessage());
            return;
        }

        // Modèles pour les tests
        Model model1 = new RandomModel();
        Model model2 = new RandomModel();

        // Tester différentes configurations de threads
        int maxThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("Début des tests de performance (max " + maxThreads + " threads)");

        // Tester pour 1 thread jusqu'au nombre max de threads disponibles
        for (int nbThreads = 1; nbThreads <= maxThreads; nbThreads++) {
            System.out.println("\nTest avec " + nbThreads + " thread(s)...");
            long totalExecutionTime = 0;
            
            // Faire plusieurs tests pour chaque configuration
            for (int test = 0; test < NB_TESTS; test++) {
                // Créer un nouveau GameStateExporter pour chaque test
                GameStateExporter exporter = new GameStateExporter(
                    "projet/src/main/ressources/evaldata_multitache/temp_game_history.csv"
                );

                // Mesurer le temps d'exécution
                long startTime = System.currentTimeMillis();
                exporter.startGamesWithUniqueStatesParallel(NB_PARTIES, model1, model2, nbThreads);
                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;
                totalExecutionTime += executionTime;

                System.out.printf("  Test %d/%d: %d ms\n", 
                    test + 1, NB_TESTS, executionTime);

                // Petite pause entre les tests
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Calculer et enregistrer la moyenne pour ce nombre de threads
            long averageTime = totalExecutionTime / NB_TESTS;
            try (FileWriter writer = new FileWriter(RESULTS_PATH, true)) {
                writer.write(String.format("%d,%d,%d\n", 
                    nbThreads, NB_PARTIES, averageTime));
            } catch (IOException e) {
                System.err.println("Erreur lors de l'écriture des résultats: " + e.getMessage());
            }
        }

        System.out.println("\nTests terminés ! Les résultats ont été sauvegardés dans : " + RESULTS_PATH);
    }
}

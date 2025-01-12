package com.parallelai.export.experimentation;

import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

import com.parallelai.export.implementations.ParallelExporter;
import com.parallelai.export.implementations.ClassicThreadExporter;
import com.parallelai.models.RandomModel;
import com.parallelai.models.utils.Model;

/**
 * Classe d'expérimentation pour évaluer l'impact du nombre de parties
 * sur les performances des différentes implémentations de parallélisation.
 * Cette classe permet de :
 * - Tester avec différentes tailles de jeux de données
 * - Maintenir un nombre fixe de threads
 * - Comparer les performances selon le volume de données
 */
public class MultiTacheExp_nbparties {
    // Configuration des tests avec différentes tailles
    private static final int[] NB_PARTIES_ARRAY = { 1000, 2000, 5000, 10000, 20000, 35000, 50000, 60000, 70000 };
    private static final int NB_TESTS = 3; // Nombre de répétitions pour la fiabilité
    private static final int WARMUP_ITERATIONS = 3; // Itérations de préchauffage
    private static final int NB_THREADS = 8; // Nombre fixe de threads
    private static final String RESULTS_PATH = "projet/src/main/ressources/evaldata_multitache/threading_performance_varied_parties.csv";

    public static void main(String[] args) {
        File directory = new File("projet/src/main/ressources/evaldata_multitache");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Créer l'en-tête du fichier CSV
        try (FileWriter writer = new FileWriter(RESULTS_PATH)) {
            writer.write("nb_parties,executor_time_ms,classic_time_ms\n");
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier CSV: " + e.getMessage());
            return;
        }

        // Modèles pour les tests
        Model model1 = new RandomModel();
        Model model2 = new RandomModel();

        System.out.println("Début des tests de performance avec " + NB_THREADS + " threads");

        // Warmup
        System.out.println("Démarrage du warmup...");
        Model warmupModel1 = new RandomModel();
        Model warmupModel2 = new RandomModel();
        String warmupPath = "projet/src/main/ressources/evaldata_multitache/warmup.csv";

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            ParallelExporter warmupExporter = new ParallelExporter(warmupPath);
            warmupExporter.startGamesWithUniqueStatesParallel(1000, warmupModel1, warmupModel2, NB_THREADS);
            System.gc(); // Force garbage collection
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Warmup terminé. Début des tests...\n");

        // Tests pour chaque configuration de parties
        for (int nbParties : NB_PARTIES_ARRAY) {
            System.out.println("\nTest avec " + nbParties + " parties...");
            long[] executorTimes = new long[NB_TESTS];
            long[] classicTimes = new long[NB_TESTS];

            String tempPath = "projet/src/main/ressources/evaldata_multitache/temp_game_history.csv";

            for (int test = 0; test < NB_TESTS; test++) {
                System.gc(); // Force garbage collection entre les tests

                // Test avec ParallelExporter
                ParallelExporter parallelExporter = new ParallelExporter(tempPath);
                long startTime = System.nanoTime(); // Plus précis que currentTimeMillis
                parallelExporter.startGamesWithUniqueStatesParallel(nbParties, model1, model2, NB_THREADS);
                executorTimes[test] = (System.nanoTime() - startTime) / 1_000_000; // Conversion en ms

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Test avec ClassicThreadExporter
                ClassicThreadExporter classicExporter = new ClassicThreadExporter(tempPath);
                startTime = System.nanoTime();
                classicExporter.startGamesWithUniqueStatesClassicThreads(nbParties, model1, model2, NB_THREADS, false);
                classicTimes[test] = (System.nanoTime() - startTime) / 1_000_000;

                System.out.printf("  Test %d/%d: ExecutorService=%d ms, Classic Threads=%d ms\n",
                        test + 1, NB_TESTS, executorTimes[test], classicTimes[test]);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            long sumExecutor = 0;
            long sumClassic = 0;
            for (int i = 0; i < NB_TESTS; i++) {
                sumExecutor += executorTimes[i];
                sumClassic += classicTimes[i];
            }
            long avgExecutor = sumExecutor / NB_TESTS;
            long avgClassic = sumClassic / NB_TESTS;

            try (FileWriter writer = new FileWriter(RESULTS_PATH, true)) {
                writer.write(String.format("%d,%d,%d\n",
                        nbParties, avgExecutor, avgClassic));
            } catch (IOException e) {
                System.err.println("Erreur lors de l'écriture des résultats: " + e.getMessage());
            }
        }

        System.out.println("\nTests terminés ! Les résultats ont été sauvegardés dans : " + RESULTS_PATH);
    }
}

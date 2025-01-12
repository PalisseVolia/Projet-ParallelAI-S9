package com.parallelai.export.experimentation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.parallelai.export.GameStateExporter;
import com.parallelai.export.implementations.ClassicThreadExporter;
import com.parallelai.export.implementations.ParallelExporter;
import com.parallelai.models.RandomModel;
import com.parallelai.models.utils.Model;

/**
 * Classe d'expérimentation pour comparer les différentes approches
 * de parallélisation et leurs performances respectives.
 * Compare :
 * - L'exécution séquentielle
 * - La parallélisation simple
 * - L'utilisation de ConcurrentHashMap
 * - L'approche sans synchronisation
 * - L'implémentation avec threads classiques
 */
public class MultiTacheExp_DiffFonc {
    // Chemins des fichiers de résultats
    private static final String RESULTS_PATH = "projet\\src\\main\\ressources\\evaldata_multitache\\performance_results.csv";
    private static final String GAME_PATH = "projet\\src\\main\\ressources\\data\\game_history.csv";
    private static final int NB_THREADS = 8;
    private static final int[] PARTY_SIZES = { 1000, 2000, 5000, 10000, 20000, 50000, 80000 };

    static class PerformanceResult {
        int nbParties;
        long seqProgressTime; // Changed to long for milliseconds
        long parProgressTime;
        long concurrentMapTime;
        long noSyncTime;
        long classicThreadTime;

        public String toCsvLine() {
            return String.format("%d,%d,%d,%d,%d,%d\n", // Changed to %d for integers
                    nbParties, seqProgressTime, parProgressTime, concurrentMapTime,
                    noSyncTime, classicThreadTime);
        }
    }

    public static void main(String[] args) {
        List<PerformanceResult> results = new ArrayList<>();

        // Initialisation des exporters
        GameStateExporter baseExporter = new ParallelExporter(GAME_PATH);
        ParallelExporter parallelExporter = new ParallelExporter(GAME_PATH);
        ClassicThreadExporter classicExporter = new ClassicThreadExporter(GAME_PATH);

        Model model1 = new RandomModel();
        Model model2 = new RandomModel();

        // Warm-up
        System.out.println("Warm-up...");
        baseExporter.startGamesWithUniqueStatesSequential(500, model1, model2);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Test pour chaque taille de parties
        for (int nbParties : PARTY_SIZES) {
            System.out.println("\n=== Test avec " + nbParties + " parties ===");
            PerformanceResult result = new PerformanceResult();
            result.nbParties = nbParties;

            // Test séquentiel avec progression
            System.out.println("Test séquentiel avec progression...");
            long startTime = System.currentTimeMillis();
            baseExporter.startGamesWithProgress(nbParties, model1, model2);
            result.seqProgressTime = System.currentTimeMillis() - startTime; // Removed division by 1000
            pause();

            // Test parallèle avec progression
            System.out.println("Test parallèle avec progression...");
            startTime = System.currentTimeMillis();
            baseExporter.startGamesParallelWithProgress(nbParties, model1, model2, NB_THREADS);
            result.parProgressTime = System.currentTimeMillis() - startTime;
            pause();

            // Test ConcurrentHashMap
            System.out.println("Test avec ConcurrentHashMap...");
            startTime = System.currentTimeMillis();
            parallelExporter.startGamesWithUniqueStatesParallel(nbParties, model1, model2, NB_THREADS);
            result.concurrentMapTime = System.currentTimeMillis() - startTime;
            pause();

            // Test sans synchronisation
            System.out.println("Test sans synchronisation...");
            startTime = System.currentTimeMillis();
            parallelExporter.startGamesWithUniqueStatesParallelNoSync(nbParties, model1, model2, NB_THREADS);
            result.noSyncTime = System.currentTimeMillis() - startTime;
            pause();

            // Test threads classiques
            System.out.println("Test avec threads classiques...");
            startTime = System.currentTimeMillis();
            classicExporter.startGamesWithUniqueStatesClassicThreads(nbParties, model1, model2, NB_THREADS, false);
            result.classicThreadTime = System.currentTimeMillis() - startTime;
            pause();

            // Afficher les résultats en cours
            System.out.printf("\nRésultats pour %d parties:\n", nbParties);
            System.out.printf("Séquentiel: %d ms\n", result.seqProgressTime);
            System.out.printf("Parallèle: %d ms\n", result.parProgressTime);
            System.out.printf("ConcurrentMap: %d ms\n", result.concurrentMapTime);
            System.out.printf("NoSync: %d ms\n", result.noSyncTime);
            System.out.printf("Classic: %d ms\n", result.classicThreadTime);

            results.add(result);
        }

        // Sauvegarder les résultats dans un CSV
        saveResults(results);

        System.out.println("\nTests terminés ! Résultats sauvegardés dans " + RESULTS_PATH);
    }

    private static void pause() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void saveResults(List<PerformanceResult> results) {
        try (FileWriter writer = new FileWriter(RESULTS_PATH)) {
            // En-tête du CSV avec indication des millisecondes
            writer.write("NbParties,SeqProgress(ms),ParProgress(ms),ConcurrentMap(ms),NoSync(ms),ClassicThread(ms)\n");

            // Données
            for (PerformanceResult result : results) {
                writer.write(result.toCsvLine());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture des résultats: " + e.getMessage());
        }
    }
}

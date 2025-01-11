package com.parallelai.export.experimentation;

import com.parallelai.export.implementations.ClassicThreadExporter;

import com.parallelai.models.RandomModel;
import com.parallelai.models.utils.Model;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Classe d'expérimentation pour comparer les performances
 * entre l'exécution mono-thread et multi-thread (8 threads).
 * Permet d'évaluer :
 * - Le gain de performance avec la parallélisation
 * - L'évolution des performances selon la taille des données
 * - L'efficacité relative des deux approches
 */
public class MultiTacheExp_1vs8 {
    
    public static void main(String[] args) {
        runExperiment();
    }
    
    public static void runExperiment() {
        // Initialisation
        Model model1 = new RandomModel();
        Model model2 = new RandomModel();
        ClassicThreadExporter exporter = new ClassicThreadExporter("not_used");
        
        // Définition des paramètres
        int[] nbParties = {1000, 5000, 10000, 50000, 100000, 500000, 1000000};
        int[] nbThreads = {1, 8};
        
        try (PrintWriter writer = new PrintWriter(new FileWriter("projet/src/main/ressources/benchmark_1vs8_threads.csv"))) {
            // En-tête du fichier CSV
            writer.println("nb_parties,nb_threads,temps_execution_ms");
            
            // Pour chaque nombre de parties
            for (int parties : nbParties) {
                // Pour chaque configuration de threads
                for (int threads : nbThreads) {
                    System.out.println("Test avec " + parties + " parties et " + threads + " threads");
                    
                    // Mesure du temps d'exécution
                    long startTime = System.currentTimeMillis();
                    exporter.startGamesNoSave(parties, model1, model2, threads);
                    long endTime = System.currentTimeMillis();
                    
                    // Calcul et enregistrement du temps d'exécution
                    long executionTime = endTime - startTime;
                    writer.println(parties + "," + threads + "," + executionTime);
                    writer.flush();
                    
                    // Petit délai pour laisser le système se stabiliser
                    Thread.sleep(1000);
                }
            }
            
            System.out.println("Expérimentation terminée. Résultats sauvegardés dans benchmark_1vs8_threads.csv");
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture des résultats : " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Expérimentation interrompue : " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}

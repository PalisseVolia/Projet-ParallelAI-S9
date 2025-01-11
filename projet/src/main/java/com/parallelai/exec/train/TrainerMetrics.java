package com.parallelai.exec.train;

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.BaseTrainingListener;
import java.text.DecimalFormat;

/**
 * Classe de suivi des métriques pendant l'entraînement d'un modèle.
 * Affiche une barre de progression et les métriques en temps réel.
 * Les métriques suivies incluent :
 * - La progression de l'entraînement (pourcentage et epochs)
 * - Loss actuelle et meilleure loss
 * - Le temps d'exécution par epoch
 */
public class TrainerMetrics extends BaseTrainingListener {
    // Variables de suivi de l'entraînement
    private final int totalEpochs;
    private int currentEpoch;
    private final DecimalFormat df;
    private long startTime;
    private double bestLoss = Double.MAX_VALUE;

    /**
     * Initialise un tracker de métriques d'entraînement.
     * 
     * @param totalEpochs Le nombre total d'epochs prévues pour l'entraînement
     */
    public TrainerMetrics(int totalEpochs) {
        this.totalEpochs = totalEpochs;
        this.currentEpoch = 0;
        this.df = new DecimalFormat("#.####");
    }

    /**
     * Appelé au début de chaque epoch.
     * Initialise les compteurs et démarre le chronomètre pour l'epoch en cours.
     * 
     * @param model Le modèle en cours d'entraînement
     */
    @Override
    public void onEpochStart(Model model) {
        currentEpoch++;
        startTime = System.currentTimeMillis();
    }

    /**
     * Appelé à la fin de chaque epoch d'entraînement.
     * Met à jour et affiche les métriques de l'epoch terminée.
     * 
     * @param model Le modèle en cours d'entraînement
     */
    @Override
    public void onEpochEnd(Model model) {
        double loss = model.score();
        bestLoss = Math.min(bestLoss, loss);
        long duration = System.currentTimeMillis() - startTime;
        
        updateProgressBar(loss, duration);
    }

    /**
     * Met à jour et affiche la barre de progression avec les métriques actuelles.
     * Affiche un résumé final lorsque l'entraînement est terminé.
     * 
     * @param loss La valeur de loss de l'epoch courante
     * @param duration La durée d'exécution de l'epoch en millisecondes
     */
    private void updateProgressBar(double loss, long duration) {
        // Calcul du pourcentage de progression
        int percentage = (currentEpoch * 100) / totalEpochs;
        int bars = percentage / 2;
        
        // Construction de la barre de progression visuelle
        StringBuilder progressBar = new StringBuilder("\r[");
        for (int i = 0; i < 50; i++) {
            progressBar.append(i < bars ? "=" : " ");
        }
        
        String metrics = String.format("] %d%% | Epoch %d/%d | Loss: %s | Best Loss: %s | Time: %.1fs ", 
            percentage, currentEpoch, totalEpochs, 
            df.format(loss), df.format(bestLoss), 
            duration / 1000.0);
            
        System.out.print(progressBar.toString() + metrics);
        
        if (currentEpoch == totalEpochs) {
            System.out.println("\nEntrainement terminé !");
            System.out.println("Final Loss: " + df.format(loss));
            System.out.println("Best Loss: " + df.format(bestLoss));
        }
    }
}

package com.parallelai.exec.train;

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.BaseTrainingListener;
import java.text.DecimalFormat;

public class TrainerMetrics extends BaseTrainingListener {
    private final int totalEpochs;
    private int currentEpoch;
    private final DecimalFormat df;
    private long startTime;
    private double bestLoss = Double.MAX_VALUE;

    public TrainerMetrics(int totalEpochs) {
        this.totalEpochs = totalEpochs;
        this.currentEpoch = 0;
        this.df = new DecimalFormat("#.####");
    }

    @Override
    public void onEpochStart(Model model) {
        currentEpoch++;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onEpochEnd(Model model) {
        double loss = model.score();
        bestLoss = Math.min(bestLoss, loss);
        long duration = System.currentTimeMillis() - startTime;
        
        updateProgressBar(loss, duration);
    }

    private void updateProgressBar(double loss, long duration) {
        int percentage = (currentEpoch * 100) / totalEpochs;
        int bars = percentage / 2;
        
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
            System.out.println("\nTraining completed!");
            System.out.println("Final Loss: " + df.format(loss));
            System.out.println("Best Loss: " + df.format(bestLoss));
        }
    }
}

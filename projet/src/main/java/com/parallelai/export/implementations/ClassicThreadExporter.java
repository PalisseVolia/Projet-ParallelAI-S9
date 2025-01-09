package com.parallelai.export.implementations;

import com.parallelai.exec.play.GameManager;
import com.parallelai.export.GameStateExporter;
import com.parallelai.export.utilities.GameExporterUtils.*;
import com.parallelai.game.Board;
import com.parallelai.models.utils.Model;
import java.util.*;

public class ClassicThreadExporter extends GameStateExporter {
    
    public ClassicThreadExporter(String outputPath) {
        super(outputPath);
    }

    public void startGamesWithUniqueStatesClassicThreads(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (version classique)...\n");
        ProgressBar.initDisplay(nbThreads);

        Thread[] threads = new Thread[nbThreads];
        @SuppressWarnings("unchecked")
        Map<String, double[]>[] threadResults = new HashMap[nbThreads];
        ProgressBar[] progressBars = new ProgressBar[nbThreads];
        
        int partiesPerThread = nbParties / nbThreads;
        final int BATCH_SIZE = 1000;

        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            threadResults[i] = new HashMap<>();
            final Map<String, double[]> localStateMap = threadResults[i];
            final StateBuffer stateBuffer = new StateBuffer();
            
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;

            progressBars[i] = new ProgressBar(partiesForThisThread, threadId);

            threads[i] = new Thread(() -> {
                int gamesCompleted = 0;
                List<GameState> batchBuffer = new ArrayList<>(BATCH_SIZE);
                
                for (int game = 0; game < partiesForThisThread; game++) {
                    Board board = new Board();
                    GameManager gameManager = new GameManager(board, model1, model2);
                    List<CompressedState> history = new ArrayList<>();

                    while (gameManager.playNextMove()) {
                        history.add(stateBuffer.compressState(board));
                    }

                    int result = calculateGameResult(board);
                    double finalResult = result == 1 ? 1.0 : result == 0 ? 0.5 : 0.0;
                    
                    batchBuffer.add(new GameState(history, result));
                    
                    if (batchBuffer.size() >= BATCH_SIZE) {
                        processBatchLocal(batchBuffer, localStateMap, finalResult);
                        batchBuffer.clear();
                    }

                    gamesCompleted++;
                    if (gamesCompleted % 100 == 0) {
                        progressBars[threadId].update(gamesCompleted);
                    }
                }
                
                if (!batchBuffer.isEmpty()) {
                    processBatchLocal(batchBuffer, localStateMap, 0.0);
                }
                
                progressBars[threadId].update(partiesForThisThread);
            });

            threads[i].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Interruption pendant l'attente des threads");
            Thread.currentThread().interrupt();
            return;
        }

        // Move cursor below progress bars
        System.out.print(String.format("\033[%dH\n", nbThreads + 2));

        List<Map<String, double[]>> allResults = new ArrayList<>(nbThreads);
        for (int i = 0; i < threadResults.length; i++) {
            allResults.add(threadResults[i]);
            threadResults[i] = null; // Libérer la mémoire immédiatement
        }

        Map<String, double[]> finalMap = new HashMap<>();
        streamMerge(allResults, finalMap);

        // Libérer la mémoire
        allResults.clear();
        allResults = null;
        System.gc();

        exportStateMap(finalMap);
        System.out.println("Terminé! " + finalMap.size() + " situations uniques sauvegardées.");
    }

    public void startGamesNoSave(int nbParties, Model model1, Model model2, int nbThreads) {
        System.out.println("Début des " + nbParties + " parties avec " + nbThreads + " threads (sans sauvegarde)...\n");
        ProgressBar.initDisplay(nbThreads);

        Thread[] threads = new Thread[nbThreads];
        int partiesPerThread = nbParties / nbThreads;

        for (int i = 0; i < nbThreads; i++) {
            final int threadId = i;
            final int partiesForThisThread = (i == nbThreads - 1) ? 
                partiesPerThread + (nbParties % nbThreads) : partiesPerThread;

            ProgressBar progressBar = new ProgressBar(partiesForThisThread, threadId);

            threads[i] = new Thread(() -> {
                int gamesCompleted = 0;
                
                for (int game = 0; game < partiesForThisThread; game++) {
                    Board board = new Board();
                    GameManager gameManager = new GameManager(board, model1, model2);
                    
                    // Jouer la partie jusqu'à la fin sans sauvegarder
                    while (gameManager.playNextMove()) {
                        // Continue jusqu'à la fin de la partie
                    }
                    
                    gamesCompleted++;
                    if (gamesCompleted % 100 == 0) {
                        progressBar.update(gamesCompleted);
                    }
                }
                
                progressBar.update(partiesForThisThread);
            });

            threads[i].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Interruption pendant l'attente des threads");
            Thread.currentThread().interrupt();
            return;
        }

        // Move cursor below progress bars
        System.out.print(String.format("\033[%dH\n", nbThreads + 2));
        System.out.println("Terminé! " + nbParties + " parties ont été jouées.");
    }

    private void processBatchLocal(List<GameState> batch, Map<String, double[]> localMap, double finalResult) {
        for (GameState game : batch) {
            for (CompressedState state : game.history) {
                String key = state.toString();
                processStateLocal(localMap, key, state, finalResult);
            }
        }
    }

    private void processStateLocal(Map<String, double[]> localMap, String key, CompressedState state, double finalResult) {
        double[] existing = localMap.get(key);
        if (existing == null) {
            double[] newState = state.decompress();
            newState[64] = finalResult;
            newState[65] = 1.0;
            localMap.put(key, newState);
        } else {
            existing[64] += finalResult;
            existing[65] += 1.0;
        }
    }
    }


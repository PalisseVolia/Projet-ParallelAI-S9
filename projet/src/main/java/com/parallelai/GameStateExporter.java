package com.parallelai;

import java.io.FileWriter;
import java.io.IOException;

public class GameStateExporter {
    private final String outputPath;

    public GameStateExporter(String outputPath) {
        this.outputPath = outputPath;
    }

    public void exportState(Board board, boolean won) {
        try (FileWriter writer = new FileWriter(outputPath, true)) {
            // Écrire l'état du plateau (64 cases)
            Disc[][] grid = board.getGrid();
            StringBuilder line = new StringBuilder();
            
            // Parcourir le plateau et convertir en format CSV
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    // Convertir Disc en valeur numérique
                    int value;
                    if (grid[i][j] == Disc.BLACK) value = 1;
                    else if (grid[i][j] == Disc.WHITE) value = -1;
                    else value = 0;
                    
                    line.append(value);
                    line.append(",");
                }
            }
            
            // Ajouter le résultat (1=victoire, 0=défaite)
            line.append(won ? "1" : "0");
            line.append("\n");
            
            writer.write(line.toString());
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
        }
    }
}

package com.parallelai;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Gestionnaire d'export des états de jeu au format CSV.
 * Cette classe permet de:
 * - Sauvegarder l'état du plateau à un moment donné
 * - Convertir les états en format exploitable pour l'apprentissage
 * - Créer un dataset d'entraînement pour le machine learning
 * 
 * Format de sortie:
 * - 64 colonnes pour l'état du plateau (1=noir, -1=blanc, 0=vide)
 * - 1 colonne pour le résultat (1=victoire, 0=défaite)
 */
public class GameStateExporter {
    /** Chemin du fichier CSV de sortie */
    private final String outputPath;

    /**
     * Crée un nouveau gestionnaire d'export.
     * 
     * @param outputPath Chemin du fichier CSV où sauvegarder les données
     */
    public GameStateExporter(String outputPath) {
        this.outputPath = outputPath;
    }

    /**
     * Exporte l'état actuel du plateau dans le fichier CSV.
     * Convertit chaque case en valeur numérique:
     * - Pion noir = 1
     * - Pion blanc = -1
     * - Case vide = 0
     * Ajoute le résultat final (victoire/défaite) comme dernière colonne.
     * 
     * @param board État du plateau à exporter
     * @param won true si la position est gagnante pour le joueur courant
     */
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

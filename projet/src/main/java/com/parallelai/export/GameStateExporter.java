package com.parallelai.export;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.parallelai.GameManager;
import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.models.Model;

/**
 * Gestionnaire d'export des états de jeu au format CSV.
 * Cette classe permet de:
 * - Sauvegarder l'état du plateau à un moment donné
 * - Convertir les états en format exploitable pour l'apprentissage
 * - Créer un dataset d'entraînement pour le machine learning
 * 
 * Format de sortie:
 * - 64 colonnes pour l'état du plateau (1=noir, -1=blanc, 0=vide)
 * - 1 colonne pour le joueur courant (-1=noir, 1=blanc)
 * - 1 colonne pour le résultat final (-1=défaite, 0=nul, 1=victoire)
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

    /**
     * Exporte une partie complète dans le fichier CSV.
     * Chaque ligne représente un état du jeu.
     * 
     * @param gameStates Liste des états de jeu
     * @param finalBoard Plateau final pour déterminer le gagnant
     */
    public void exportGame(List<Board> gameStates, Board finalBoard) {
        try (FileWriter writer = new FileWriter(outputPath, true)) {
            int finalResult = calculateGameResult(finalBoard);
            boolean isBlackTurn = true; // Le noir commence toujours
            
            // Écrire chaque état du jeu
            for (int i = 0; i < gameStates.size(); i++) {
                Board board = gameStates.get(i);
                
                // Ne sauvegarder que les états correspondant au joueur courant
                if ((i % 2 == 0 && !isBlackTurn) || (i % 2 == 1 && isBlackTurn)) {
                    continue;
                }

                StringBuilder line = new StringBuilder();
                Disc[][] grid = board.getGrid();
                
                // Écriture de l'état du plateau (64 colonnes)
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        int value = grid[row][col] == Disc.BLACK ? 1 : 
                                  grid[row][col] == Disc.WHITE ? -1 : 0;
                        line.append(value).append(",");
                    }
                }
                
                // Ajouter le joueur courant (-1 pour noir, 1 pour blanc)
                line.append(isBlackTurn ? "-1," : "1,");
                
                // Ajouter le résultat final (-1 défaite, 0 nul, 1 victoire)
                if (isBlackTurn) {
                    line.append(finalResult);
                } else {
                    line.append(-finalResult); // Inverser le résultat pour le joueur blanc
                }
                
                line.append("\n");
                writer.write(line.toString());
                
                isBlackTurn = !isBlackTurn; // Alterner les tours
            }
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
        }
    }

   
    /**
     * Joue et exporte plusieurs parties entre deux modèles
     * @param nbParties Nombre de parties à jouer
     * @param model1 Premier modèle (joue les noirs)
     * @param model2 Second modèle (joue les blancs)
     */
    public void startGames(int nbParties, Model model1, Model model2) {
        System.out.println("Début des " + nbParties + " parties...");
        
        for (int i = 0; i < nbParties; i++) {
            // Initialiser une nouvelle partie
            Board board = new Board();
            GameManager game = new GameManager(board, model1, model2);
            
            // Jouer la partie complète
            game.startGame();
            
            if ((i + 1) % 10 == 0) {
                System.out.println("Progression : " + (i + 1) + "/" + nbParties + " parties terminées");
            }
        }
        
        System.out.println("Terminé! " + nbParties + " parties ont été sauvegardées dans " + outputPath);
    }

    /**
     * Calcule le résultat final de la partie
     * @param finalBoard Plateau final
     * @return 1 pour victoire des noirs, -1 pour victoire des blancs, 0 pour égalité
     */
    private int calculateGameResult(Board finalBoard) {
        int blackCount = finalBoard.getDiscCount(Disc.BLACK);
        int whiteCount = finalBoard.getDiscCount(Disc.WHITE);
        
        if (blackCount > whiteCount) return 1;
        if (whiteCount > blackCount) return -1;
        return 0;
    }
}

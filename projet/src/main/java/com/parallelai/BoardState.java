package com.parallelai;

/**
 * Représente un instantané de l'état du plateau de jeu Othello.
 * Cette classe permet de:
 * - Sauvegarder l'état complet du plateau à un moment donné
 * - Conserver l'historique des positions pendant une partie
 * - Immutable: une fois créé, l'état ne peut pas être modifié
 */
public class BoardState {
    /** 
     * État du plateau sous forme de tableau 2D de pions.
     * Copie profonde de la grille originale pour garantir l'immutabilité.
     */
    private final Disc[][] state;

    /**
     * Crée une nouvelle capture de l'état du plateau.
     * Effectue une copie profonde de la grille pour éviter les modifications externes.
     * 
     * @param board Le plateau de jeu dont l'état doit être sauvegardé
     */
    public BoardState(Board board) {
        Disc[][] originalGrid = board.getGrid();
        state = new Disc[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                state[i][j] = originalGrid[i][j];
            }
        }
    }

    /**
     * Renvoie l'état sauvegardé du plateau.
     * 
     * @return Une référence au tableau 2D représentant l'état
     */
    public Disc[][] getState() {
        return state;
    }
}

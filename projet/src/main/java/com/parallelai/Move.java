package com.parallelai;

/**
 * Représente un coup dans le jeu d'Othello.
 * Cette classe encapsule:
 * - La position du coup (ligne et colonne)
 * - La couleur du pion à placer
 * Classe immutable: les attributs sont finals et ne peuvent pas être modifiés
 * après création.
 */
public class Move {
    /** Numéro de ligne où placer le pion (0-7) */
    public final int row;
    
    /** Numéro de colonne où placer le pion (0-7) */
    public final int col;
    
    /** Couleur du pion à placer (NOIR ou BLANC) */
    public final Disc color;

    /**
     * Crée un nouveau coup avec une position et une couleur.
     * 
     * @param row Numéro de ligne (0-7)
     * @param col Numéro de colonne (0-7) 
     * @param color Couleur du pion à placer
     */
    public Move(int row, int col, Disc color) {
        this.row = row;
        this.col = col;
        this.color = color;
    }
}

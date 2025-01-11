package com.parallelai.game;

/**
 * Représente les différents états possibles d'une case du plateau Othello.
 * Cette énumération définit :
 * - Les trois états possibles d'une case (vide, noir, blanc)
 * - Le symbole d'affichage associé à chaque état
 * - La méthode pour obtenir la couleur opposée
 */
public enum Disc {
    /** Case vide représentée par '.' */
    EMPTY('.'),
    
    /** Pion noir représenté par 'B' */
    BLACK('B'),
    
    /** Pion blanc représenté par 'W' */
    WHITE('W');

    /** Symbole utilisé pour l'affichage de cet état */
    private final char symbol;

    /**
     * Crée un nouvel état avec son symbole d'affichage.
     * 
     * @param symbol Le caractère représentant cet état
     */
    Disc(char symbol) {
        this.symbol = symbol;
    }

    /**
     * Renvoie le symbole associé à cet état.
     * 
     * @return Le caractère représentant cet état
     */
    public char getSymbol() {
        return symbol;
    }

    /**
     * Renvoie la couleur opposée du pion.
     * - Pour un pion noir, renvoie blanc
     * - Pour un pion blanc, renvoie noir
     * - Pour une case vide, renvoie vide
     * 
     * @return La couleur opposée ou EMPTY si la case est vide
     */
    public Disc opposite() {
        return this == BLACK ? WHITE : (this == WHITE ? BLACK : EMPTY);
    }
}

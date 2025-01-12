package com.parallelai.exec.play;

/**
 * Énumération représentant les différents résultats possibles d'une partie
 * d'Othello
 */
public enum GameResult {
    /**
     * Indique que le joueur avec les pions noirs a gagné la partie
     */
    BLACK_WINS,

    /**
     * Indique que le joueur avec les pions blancs a gagné la partie
     */
    WHITE_WINS,

    /**
     * Indique que la partie s'est terminée sur une égalité
     */
    TIE
}

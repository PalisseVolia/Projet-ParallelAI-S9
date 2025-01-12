package com.parallelai.models;

import com.parallelai.game.Board;
import com.parallelai.game.Move;
import com.parallelai.models.utils.Model;

/**
 * Modèle qui attribue une probabilité égale à tous les coups possibles.
 */
public class RandomModel implements Model {
    /**
     * Évalue un coup en retournant toujours une valeur constante.
     *
     * @param move  Le coup à évaluer
     * @param board L'état actuel du plateau
     * @return 0.5, indiquant une probabilité égale pour tous les coups
     */
    @Override
    public double evaluateMove(Move move, Board board) {
        return 0.5; // Tous les coups ont la même probabilité
    }
}
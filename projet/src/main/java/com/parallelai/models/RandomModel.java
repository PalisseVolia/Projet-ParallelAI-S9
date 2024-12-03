
package com.parallelai.models;

import com.parallelai.game.Board;
import com.parallelai.game.Move;

/**
 * Model that assigns equal probability to all moves.
 */
public class RandomModel implements Model {
    @Override
    public double evaluateMove(Move move, Board board) {
        return 0.5; // All moves are equally likely
    }
}
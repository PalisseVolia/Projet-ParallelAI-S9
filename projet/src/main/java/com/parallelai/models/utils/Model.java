
package com.parallelai.models.utils;

import com.parallelai.game.Board;
import com.parallelai.game.Move;

/**
 * Interface for game state evaluation models.
 * Each model implements its own strategy for evaluating positions.
 */
public interface Model {
    /**
     * Evaluates a potential move and returns a score between 0 and 1.
     * Higher scores indicate better moves according to the model's strategy.
     * 
     * @param move The move to evaluate
     * @param board Current state of the board
     * @return Evaluation score between 0 (worst) and 1 (best)
     */
    double evaluateMove(Move move, Board board);
}
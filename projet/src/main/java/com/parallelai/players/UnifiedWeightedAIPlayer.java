package com.parallelai.players;

import java.util.ArrayList;
import java.util.List;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;
import com.parallelai.models.utils.Model;

/**
 * Unified AI player that uses a model to evaluate and select moves.
 */
public class UnifiedWeightedAIPlayer extends Player {
    private final Model model;

    public UnifiedWeightedAIPlayer(Disc color, Model model) {
        super(color);
        this.model = model;
    }

    @Override
    public Move getMove(Board board) {
        List<Move> validMoves = new ArrayList<>();
        List<Double> evaluations = new ArrayList<>();
        
        // Find valid moves and get their evaluations from the model
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Move move = new Move(i, j, color);
                if (board.isValidMove(move, color)) {
                    validMoves.add(move);
                    double eval = model.evaluateMove(move, board);
                    // Invert evaluation for white player
                    if (color == Disc.WHITE) {
                        eval = 1.0 - eval;
                    }
                    evaluations.add(eval);
                }
            }
        }

        if (validMoves.isEmpty()) {
            return null;
        }

        // Calculate total weight (sum of all evaluations)
        double totalWeight = evaluations.stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        // Generate random value between 0 and total weight
        double random = Math.random() * totalWeight;

        // Use weighted random selection
        double weightSum = 0;
        for (int i = 0; i < evaluations.size(); i++) {
            weightSum += evaluations.get(i);
            if (random <= weightSum) {
                return validMoves.get(i);
            }
        }

        // Fallback to last move (should rarely happen due to floating point precision)
        return validMoves.get(validMoves.size() - 1);
    }
}
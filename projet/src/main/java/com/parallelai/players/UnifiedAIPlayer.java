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
public class UnifiedAIPlayer extends Player {
    private final Model model;

    public UnifiedAIPlayer(Disc color, Model model) {
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
                    evaluations.add(model.evaluateMove(move, board));
                }
            }
        }

        if (validMoves.isEmpty()) {
            return null;
        }

        // Find best score
        double bestScore = evaluations.get(0);
        for (int i = 1; i < evaluations.size(); i++) {
            if (evaluations.get(i) > bestScore) {
                bestScore = evaluations.get(i);
            }
        }

        // Collect all moves with the best score
        List<Move> bestMoves = new ArrayList<>();
        for (int i = 0; i < evaluations.size(); i++) {
            if (evaluations.get(i) == bestScore) {
                bestMoves.add(validMoves.get(i));
            }
        }

        // Randomly select one of the best moves
        int randomIndex = (int)(Math.random() * bestMoves.size());
        return bestMoves.get(randomIndex);
    }
}
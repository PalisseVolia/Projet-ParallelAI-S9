
package com.parallelai.models;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;

/**
 * Model that uses minimax algorithm to evaluate moves.
 */
public class MinimaxModel implements Model {
    private static final int MAX_DEPTH = 4;

    @Override
    public double evaluateMove(Move move, Board board) {
        // Create a temporary board to simulate the move
        Board tempBoard = new Board();
        tempBoard.makeMove(move);
        
        // Get minimax score and normalize to [0,1] range
        int score = minimax(tempBoard, MAX_DEPTH, false, move.color);
        return normalizeScore(score);
    }

    private int minimax(Board board, int depth, boolean isMaximizing, Disc originalColor) {
        if (depth == 0) {
            return evaluateBoard(board, originalColor);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    Move move = new Move(i, j, originalColor);
                    if (board.isValidMove(move, originalColor)) {
                        Board tempBoard = new Board();
                        tempBoard.makeMove(move);
                        int eval = minimax(tempBoard, depth - 1, false, originalColor);
                        maxEval = Math.max(maxEval, eval);
                    }
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            Disc opponent = originalColor.opposite();
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    Move move = new Move(i, j, opponent);
                    if (board.isValidMove(move, opponent)) {
                        Board tempBoard = new Board();
                        tempBoard.makeMove(move);
                        int eval = minimax(tempBoard, depth - 1, true, originalColor);
                        minEval = Math.min(minEval, eval);
                    }
                }
            }
            return minEval;
        }
    }

    private int evaluateBoard(Board board, Disc color) {
        int score = 0;
        Disc[][] grid = board.getGrid();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (grid[i][j] == color) {
                    score++;
                } else if (grid[i][j] == color.opposite()) {
                    score--;
                }
            }
        }
        return score;
    }

    private double normalizeScore(int score) {
        // Convert minimax score to range [0,1]
        // This is a simple example - adjust based on your score range
        return (score + 64) / 128.0;
    }
}
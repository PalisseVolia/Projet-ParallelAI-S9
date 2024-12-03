package com.parallelai;

public class AIPlayer extends Player {
    private static final int MAX_DEPTH = 4;

    public AIPlayer(Disc color) {
        super(color);
    }

    @Override
    public Move getMove(Board board) {
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Move move = new Move(i, j, color);
                if (board.isValidMove(move, color)) {
                    Board tempBoard = new Board();
                    tempBoard.makeMove(move);
                    int score = minimax(tempBoard, MAX_DEPTH, false);
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = move;
                    }
                }
            }
        }
        return bestMove;
    }

    private int minimax(Board board, int depth, boolean isMaximizing) {
        if (depth == 0) {
            return evaluateBoard(board);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    Move move = new Move(i, j, color);
                    if (board.isValidMove(move, color)) {
                        Board tempBoard = new Board();
                        tempBoard.makeMove(move);
                        int eval = minimax(tempBoard, depth - 1, false);
                        maxEval = Math.max(maxEval, eval);
                    }
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            Disc opponent = color.opposite();
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    Move move = new Move(i, j, opponent);
                    if (board.isValidMove(move, opponent)) {
                        Board tempBoard = new Board();
                        tempBoard.makeMove(move);
                        int eval = minimax(tempBoard, depth - 1, true);
                        minEval = Math.min(minEval, eval);
                    }
                }
            }
            return minEval;
        }
    }

    private int evaluateBoard(Board board) {
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
}

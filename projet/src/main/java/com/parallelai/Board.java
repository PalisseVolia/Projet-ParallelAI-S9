package com.parallelai;



public class Board {
    private static final int SIZE = 8;
    private Disc[][] grid;
    private static final int[][] DIRECTIONS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        {0, -1},           {0, 1},
        {1, -1},  {1, 0},  {1, 1}
    };

    public Board() {
        grid = new Disc[SIZE][SIZE];
        initializeBoard();
    }

    private void initializeBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = Disc.EMPTY;
            }
        }
        grid[3][3] = Disc.WHITE;
        grid[3][4] = Disc.BLACK;
        grid[4][3] = Disc.BLACK;
        grid[4][4] = Disc.WHITE;
    }

    public void display() {
        System.out.println("  0 1 2 3 4 5 6 7");
        for (int i = 0; i < SIZE; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < SIZE; j++) {
                System.out.print(grid[i][j].getSymbol() + " ");
            }
            System.out.println();
        }
    }

    public boolean isValidMove(Move move, Disc color) {
        if (!isInBounds(move) || grid[move.row][move.col] != Disc.EMPTY) {
            return false;
        }

        for (int[] direction : DIRECTIONS) {
            if (wouldFlip(move, direction, color)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInBounds(Move move) {
        return move.row >= 0 && move.row < SIZE && 
               move.col >= 0 && move.col < SIZE;
    }

    private boolean wouldFlip(Move move, int[] direction, Disc color) {
        int row = move.row + direction[0];
        int col = move.col + direction[1];
        boolean foundOpponent = false;

        while (row >= 0 && row < SIZE && col >= 0 && col < SIZE) {
            if (grid[row][col] == Disc.EMPTY) return false;
            if (grid[row][col] == color.opposite()) {
                foundOpponent = true;
            } else if (grid[row][col] == color && foundOpponent) {
                return true;
            } else {
                return false;
            }
            row += direction[0];
            col += direction[1];
        }
        return false;
    }

    public void makeMove(Move move) {
        grid[move.row][move.col] = move.color;
        for (int[] direction : DIRECTIONS) {
            flipDiscs(move, direction);
        }
    }

    private void flipDiscs(Move move, int[] direction) {
        if (!wouldFlip(move, direction, move.color)) return;

        int row = move.row + direction[0];
        int col = move.col + direction[1];

        while (grid[row][col] == move.color.opposite()) {
            grid[row][col] = move.color;
            row += direction[0];
            col += direction[1];
        }
    }

    public boolean hasValidMoves(Disc color) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (isValidMove(new Move(i, j, color), color)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getDiscCount(Disc color) {
        int count = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j] == color) {
                    count++;
                }
            }
        }
        return count;
    }

    public Disc[][] getGrid() {
        return grid;
    }
}

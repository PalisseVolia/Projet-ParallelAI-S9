package com.parallelai;

public class BoardState {
    private final Disc[][] state;

    public BoardState(Board board) {
        Disc[][] originalGrid = board.getGrid();
        state = new Disc[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                state[i][j] = originalGrid[i][j];
            }
        }
    }

    public Disc[][] getState() {
        return state;
    }
}

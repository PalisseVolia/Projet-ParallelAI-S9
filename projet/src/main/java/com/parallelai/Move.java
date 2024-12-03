package com.parallelai;

public class Move {
    public final int row;
    public final int col;
    public final Disc color;

    public Move(int row, int col, Disc color) {
        this.row = row;
        this.col = col;
        this.color = color;
    }
}

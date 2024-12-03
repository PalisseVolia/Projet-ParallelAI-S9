package com.parallelai;

public abstract class Player {
    protected Disc color;

    public Player(Disc color) {
        this.color = color;
    }

    public abstract Move getMove(Board board);

    public Disc getColor() {
        return color;
    }
}

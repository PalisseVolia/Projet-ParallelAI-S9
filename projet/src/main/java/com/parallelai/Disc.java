package com.parallelai;

public enum Disc {
    EMPTY('.'),
    BLACK('B'),
    WHITE('W');

    private final char symbol;

    Disc(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public Disc opposite() {
        return this == BLACK ? WHITE : (this == WHITE ? BLACK : EMPTY);
    }
}

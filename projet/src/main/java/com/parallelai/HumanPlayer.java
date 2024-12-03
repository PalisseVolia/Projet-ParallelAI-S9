package com.parallelai;

import java.util.Scanner;

public class HumanPlayer extends Player {
    private Scanner scanner;

    public HumanPlayer(Disc color, Scanner scanner) {
        super(color);
        this.scanner = scanner;
    }

    @Override
    public Move getMove(Board board) {
        int row, col;
        Move move;
        do {
            System.out.println("Enter row (0-7): ");
            row = scanner.nextInt();
            System.out.println("Enter column (0-7): ");
            col = scanner.nextInt();
            move = new Move(row, col, color);
        } while (!board.isValidMove(move, color));

        return move;
    }
}

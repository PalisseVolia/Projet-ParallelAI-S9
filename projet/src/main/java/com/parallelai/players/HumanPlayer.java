package com.parallelai.players;

import java.util.Scanner;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;

/**
 * Représente un joueur humain dans le jeu d'Othello.
 * Cette classe permet de:
 * - Gérer les entrées utilisateur via console
 * - Valider les coups saisis par le joueur
 * - Demander une nouvelle saisie si le coup est invalide
 */
public class HumanPlayer extends Player {
    /** Scanner pour lire les entrées utilisateur */
    private Scanner scanner;

    /**
     * Crée un nouveau joueur humain.
     * 
     * @param color La couleur des pions du joueur (NOIR ou BLANC)
     * @param scanner Le scanner pour lire les entrées console
     */
    public HumanPlayer(Disc color, Scanner scanner) {
        super(color);
        this.scanner = scanner;
    }

    /**
     * Demande au joueur de saisir un coup via la console.
     * Continue à demander jusqu'à obtenir un coup valide.
     * Format attendu:
     * - Ligne: nombre entre 0 et 7
     * - Colonne: nombre entre 0 et 7
     * 
     * @param board État actuel du plateau de jeu
     * @return Le coup valide choisi par le joueur
     */
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

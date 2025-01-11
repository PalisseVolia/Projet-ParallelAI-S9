package com.parallelai.players;

import java.util.Scanner;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;

/**
 * Représente un joueur humain dans le jeu d'Othello.
 * Cette classe gère l'interaction avec un joueur humain via la console,
 * notamment la saisie et la validation des coups.
 */
public class HumanPlayer extends Player {
    /** Scanner utilisé pour lire les entrées utilisateur depuis la console */
    private Scanner scanner;

    /**
     * Crée un nouveau joueur humain avec une couleur spécifique.
     * 
     * @param color La couleur des pions du joueur (NOIR ou BLANC)
     * @param scanner Le scanner pour lire les entrées console, ne doit pas être null
     */
    public HumanPlayer(Disc color, Scanner scanner) {
        super(color);
        this.scanner = scanner;
    }

    /**
     * Demande au joueur de saisir un coup via la console.
     * Continue à demander jusqu'à obtenir un coup valide.
     * 
     * Format de saisie :
     * - Ligne : nombre entier entre 0 et 7 inclus
     * - Colonne : nombre entier entre 0 et 7 inclus
     * 
     * Le coup doit être valide selon les règles d'Othello.
     * 
     * @param board État actuel du plateau de jeu
     * @return Le coup valide choisi par le joueur
     */
    @Override
    public Move getMove(Board board) {
        int row, col;
        Move move;
        do {
            System.out.println("Entrez la ligne (0-7) : ");
            row = scanner.nextInt();
            System.out.println("Entrez la colonne (0-7) : ");
            col = scanner.nextInt();
            move = new Move(row, col, color);
        } while (!board.isValidMove(move, color));

        return move;
    }
}

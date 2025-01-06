package com.parallelai.game;

/**
 * Représentation du plateau de jeu d'Othello.
 * Cette classe gère:
 * - L'état du plateau (8x8 cases)
 * - La validation des coups
 * - L'application des règles du jeu
 * - Le comptage des pions
 */
public class Board {
    /** Taille du plateau (8x8) */
    private static final int SIZE = 8;
    
    /** Directions possibles pour retourner les pions */
    private static final int[][] DIRECTIONS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        {0, -1},           {0, 1},
        {1, -1},  {1, 0},  {1, 1}
    };
    
    /** Grille du plateau stockant les pions */
    private Disc[][] grid;

    /**
     * Crée un nouveau plateau dans sa configuration initiale:
     * - 4 pions au centre (2 noirs, 2 blancs)
     * - Le reste des cases vides
     */
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

    /**
     * Resets the board to its initial state with the four center pieces
     */
    public void reset() {
        initializeBoard();
    }

    /**
     * Affiche l'état actuel du plateau.
     * Montre les pions noirs (X), blancs (O) et cases vides (.).
     */
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

    /**
     * Vérifie si un coup est valide selon les règles d'Othello.
     * Un coup est valide s'il permet de retourner au moins un pion adverse.
     * 
     * @param move Le coup à vérifier
     * @param color La couleur du joueur qui joue
     * @return true si le coup est valide
     */
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

    /**
     * Vérifie si des pions seraient retournés dans une direction donnée.
     * 
     * @param move Le coup à vérifier
     * @param direction La direction à explorer
     * @param color La couleur du joueur
     * @return true si des pions peuvent être retournés
     */
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

    /**
     * Applique un coup sur le plateau en retournant les pions nécessaires.
     * 
     * @param move Le coup à jouer
     */
    public void makeMove(Move move) {
        grid[move.row][move.col] = move.color;
        for (int[] direction : DIRECTIONS) {
            flipDiscs(move, direction);
        }
    }

    /**
     * Retourne les pions dans une direction donnée suite à un coup.
     * 
     * @param move Le coup joué
     * @param direction La direction où retourner les pions
     */
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

    /**
     * Vérifie si un joueur a au moins un coup valide disponible.
     * 
     * @param color La couleur du joueur à vérifier
     * @return true si le joueur peut jouer
     */
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

    /**
     * Compte le nombre de pions d'une couleur donnée sur le plateau.
     * 
     * @param color La couleur des pions à compter
     * @return Le nombre de pions de cette couleur
     */
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

    /**
     * Renvoie la grille représentant le plateau.
     * 
     * @return Le tableau 2D contenant les pions
     */
    public Disc[][] getGrid() {
        return grid;
    }

    /**
     * Crée une copie profonde du plateau actuel
     * @return Une nouvelle instance de Board avec le même état
     */
    public Board copy() {
        Board newBoard = new Board();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                newBoard.grid[i][j] = this.grid[i][j];
            }
        }
        return newBoard;
    }
}

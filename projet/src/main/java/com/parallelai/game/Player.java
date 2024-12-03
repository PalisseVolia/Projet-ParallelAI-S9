package com.parallelai.game;

/**
 * Classe abstraite représentant un joueur d'Othello.
 * Cette classe définit:
 * - L'interface commune à tous les types de joueurs (humain ou IA)
 * - La gestion de la couleur des pions du joueur
 * - La méthode abstraite pour obtenir le prochain coup
 */
public abstract class Player {
    /** Couleur des pions du joueur (NOIR ou BLANC) */
    protected Disc color;

    /**
     * Crée un nouveau joueur avec une couleur donnée.
     * 
     * @param color La couleur des pions du joueur
     */
    public Player(Disc color) {
        this.color = color;
    }

    /**
     * Détermine le prochain coup à jouer.
     * Méthode abstraite à implémenter par les classes dérivées
     * selon leur stratégie spécifique.
     * 
     * @param board État actuel du plateau de jeu
     * @return Le coup choisi par le joueur
     */
    public abstract Move getMove(Board board);

    /**
     * Renvoie la couleur des pions du joueur.
     * 
     * @return La couleur (NOIR ou BLANC) associée au joueur
     */
    public Disc getColor() {
        return color;
    }
}

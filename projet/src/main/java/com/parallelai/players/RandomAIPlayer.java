package com.parallelai.players;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;

/**
 * Implémentation d'un joueur IA qui joue de façon aléatoire.
 * Cette classe permet de:
 * - Identifier les coups valides possibles
 * - Assigner une évaluation uniforme à chaque coup (0.5)
 * - Sélectionner un coup aléatoirement parmi les coups valides
 * Cette implémentation servira de base pour des stratégies plus avancées
 * où les évaluations seront fournies par un modèle d'apprentissage.
 */

public class RandomAIPlayer extends Player {
    /** Générateur de nombres aléatoires pour la sélection des coups */
    private final Random random;
    
    /**
     * Crée un nouveau joueur IA aléatoire.
     * @param color La couleur des pions du joueur (NOIR ou BLANC)
     */
    public RandomAIPlayer(Disc color) {
        super(color);
        this.random = new Random();
    }

    /**
     * Détermine le prochain coup à jouer.
     * 1. Trouve tous les coups valides sur le plateau
     * 2. Assigne une évaluation de 0.5 à chaque coup
     * 3. Sélectionne un coup au hasard parmi les possibilités
     * 
     * @param board État actuel du plateau de jeu
     * @return Le coup choisi, ou null si aucun coup n'est possible
     */
    @Override
    public Move getMove(Board board) {
        // Récupérer tous les coups valides
        List<Move> validMoves = new ArrayList<>();
        List<Double> evaluations = new ArrayList<>();
        
        // Trouver les coups valides et leurs évaluations
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Move move = new Move(i, j, color);
                if (board.isValidMove(move, color)) {
                    validMoves.add(move);
                    // Pour l'instant, chaque coup a une évaluation de 0.5
                    evaluations.add(0.5);
                }
            }
        }
        
        if (validMoves.isEmpty()) {
            return null;
        }
        
        // Sélection pondérée aléatoire
        double totalWeight = evaluations.stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalWeight;
        
        double cumulativeWeight = 0;
        for (int i = 0; i < validMoves.size(); i++) {
            cumulativeWeight += evaluations.get(i);
            if (randomValue <= cumulativeWeight) {
                return validMoves.get(i);
            }
        }
        
        // En cas d'erreur de calcul, retourner le dernier coup valide
        return validMoves.get(validMoves.size() - 1);
    }
    
    // Pour plus tard: fonction à remplacer par le vrai modèle
    protected double evaluateMove(Move move, Board board) {
        return 0.5; // Pour l'instant, retourne toujours 0.5
    }
}
package com.parallelai.players;

import java.util.ArrayList;
import java.util.List;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;
import com.parallelai.models.utils.Model;

/**
 * Joueur IA qui utilise un modèle pour évaluer et sélectionner les coups.
 */
public class AIWeightedPlayer extends Player {
    private final Model model;
    // La température contrôle la distribution des poids exponentiels
    // Les valeurs plus élevées rendent la probabilité de sélectionner un meilleur coup plsu élevée
    private final double temperature = 5.0;

    /**
     * Crée un nouveau joueur IA avec un modèle d'évaluation spécifique.
     *
     * @param color La couleur des pions du joueur (NOIR ou BLANC)
     * @param model Le modèle utilisé pour évaluer les coups
     */
    public AIWeightedPlayer(Disc color, Model model) {
        super(color);
        this.model = model;
    }

    /**
     * Détermine et retourne le prochain coup à jouer en utilisant le modèle d'évaluation.
     * Le coup est choisi de manière pondérée en fonction des évaluations du modèle.
     *
     * @param board L'état actuel du plateau de jeu
     * @return Le coup choisi, ou null si aucun coup n'est possible
     */
    @Override
    public Move getMove(Board board) {
        List<Move> validMoves = new ArrayList<>();
        List<Double> evaluations = new ArrayList<>();
        
        // Collecte des coups et leurs évaluations
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Move move = new Move(i, j, color);
                if (board.isValidMove(move, color)) {
                    validMoves.add(move);
                    double eval = model.evaluateMove(move, board);
                    if (color == Disc.WHITE) {
                        eval = 1.0 - eval;
                    }
                    evaluations.add(eval);
                }
            }
        }

        if (validMoves.isEmpty()) {
            return null;
        }

        // Application de la pondération exponentielle
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0.0;
        for (Double eval : evaluations) {
            double weight = Math.exp(eval * temperature);
            weights.add(weight);
            totalWeight += weight;
        }

        // Sélection aléatoire pondérée
        double random = Math.random() * totalWeight;
        double weightSum = 0;
        for (int i = 0; i < weights.size(); i++) {
            weightSum += weights.get(i);
            if (random <= weightSum) {
                return validMoves.get(i);
            }
        }

        return validMoves.get(validMoves.size() - 1);
    }
}
package com.parallelai.players;

import java.util.ArrayList;
import java.util.List;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;
import com.parallelai.models.utils.Model;

/**
 * Joueur IA qui utilise un modèle pour évaluer et sélectionner les meilleurs
 * coups.
 */
public class AIPlayer extends Player {
    public final Model model;

    /**
     * Crée un nouveau joueur IA avec un modèle d'évaluation spécifique.
     * 
     * @param color La couleur des pions du joueur (NOIR ou BLANC)
     * @param model Le modèle utilisé pour évaluer les coups
     */
    public AIPlayer(Disc color, Model model) {
        super(color);
        this.model = model;
    }

    /**
     * Détermine et retourne le meilleur coup à jouer selon le modèle d'évaluation.
     * SI plusieurs coups ont le même score, en sélectionne un aléatoirement.
     *
     * @param board L'état actuel du plateau de jeu
     * @return Le meilleur coup choisi, ou null si aucun coup n'est possible
     */
    @Override
    public Move getMove(Board board) {
        List<Move> validMoves = new ArrayList<>();
        List<Double> evaluations = new ArrayList<>();

        // Recherche des coups valides et obtention de leurs évaluations depuis le
        // modèle
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

        // Trouve le meilleur score
        double bestScore = evaluations.get(0);
        for (int i = 1; i < evaluations.size(); i++) {
            if (evaluations.get(i) > bestScore) {
                bestScore = evaluations.get(i);
            }
        }

        // Collecte tous les coups ayant le meilleur score
        List<Move> bestMoves = new ArrayList<>();
        for (int i = 0; i < evaluations.size(); i++) {
            if (evaluations.get(i) == bestScore) {
                bestMoves.add(validMoves.get(i));
            }
        }

        // Sélectionne aléatoirement l'un des meilleurs coups
        int randomIndex = (int) (Math.random() * bestMoves.size());
        return bestMoves.get(randomIndex);
    }
}
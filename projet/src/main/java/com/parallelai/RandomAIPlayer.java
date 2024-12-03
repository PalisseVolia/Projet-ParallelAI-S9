package com.parallelai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomAIPlayer extends Player {
    private final Random random;
    
    public RandomAIPlayer(Disc color) {
        super(color);
        this.random = new Random();
    }

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
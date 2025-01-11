package com.parallelai.models.utils;

import com.parallelai.game.Board;
import com.parallelai.game.Move;

/**
 * Interface pour les modèles d'évaluation des états de jeu.
 * Chaque modèle implémente sa propre stratégie d'évaluation des positions.
 */
public interface Model {
    /**
     * Évalue un coup potentiel et retourne un score entre 0 et 1.
     * Les scores plus élevés indiquent de meilleurs coups selon la stratégie du modèle.
     * 
     * @param move Le coup à évaluer
     * @param board État actuel du plateau
     * @return Score d'évaluation entre 0 (pire) et 1 (meilleur)
     */
    double evaluateMove(Move move, Board board);
    
    /**
     * Retourne le nom de l'implémentation du modèle.
     * @return Chaîne représentant le nom du modèle
     */
    default String getName() {
        return this.getClass().getSimpleName().replace("Model", "");
    }
}
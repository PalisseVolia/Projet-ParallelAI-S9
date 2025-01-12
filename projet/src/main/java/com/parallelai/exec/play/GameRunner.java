package com.parallelai.exec.play;

import java.util.concurrent.Callable;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;
import com.parallelai.models.utils.Model;
import com.parallelai.players.*;

/**
 * Classe responsable de l'exécution d'une partie d'Othello entre deux IA
 * Implémente Callable pour permettre une exécution parallèle des parties
 */
public class GameRunner implements Callable<GameResult> {
    private final Model model1;
    private final Model model2;
    private final AIType aiType;
    private final Board localBoard;
    private Player localPlayer1;
    private Player localPlayer2;
    private Player localCurrentPlayer;
    private final Runnable progressCallback;

    /**
     * Types d'IA disponibles pour le jeu
     */
    public enum AIType {
        REGULAR, WEIGHTED
    }

    /**
     * Constructeur initialisant une nouvelle partie
     * 
     * @param model1           Modèle pour le premier joueur
     * @param model2           Modèle pour le second joueur
     * @param aiType           Type d'IA à utiliser
     * @param progressCallback Callback appelé à la fin de chaque partie
     */
    public GameRunner(Model model1, Model model2, AIType aiType, Runnable progressCallback) {
        this.model1 = model1;
        this.model2 = model2;
        this.aiType = aiType;
        this.localBoard = new Board();
        this.progressCallback = progressCallback;
        setupLocalPlayers();
    }

    /**
     * Configure les joueurs locaux selon le type d'IA choisi
     */
    private void setupLocalPlayers() {
        if (aiType == AIType.REGULAR) {
            localPlayer1 = new AIPlayer(Disc.BLACK, model1);
            localPlayer2 = new AIPlayer(Disc.WHITE, model2);
        } else {
            localPlayer1 = new AIWeightedPlayer(Disc.BLACK, model1);
            localPlayer2 = new AIWeightedPlayer(Disc.WHITE, model2);
        }
        localCurrentPlayer = localPlayer1;
    }

    /**
     * Exécute la partie jusqu'à sa fin et retourne le résultat
     * 
     * @return Le résultat de la partie (victoire noire, blanche ou égalité)
     */
    @Override
    public GameResult call() {
        // Boucle principale du jeu
        while (true) {
            if (!processLocalMove()) {
                break;
            }
            // Alternance des joueurs
            localCurrentPlayer = (localCurrentPlayer == localPlayer1) ? localPlayer2 : localPlayer1;
        }

        // Calcul du score final
        int blackCount = localBoard.getDiscCount(Disc.BLACK);
        int whiteCount = localBoard.getDiscCount(Disc.WHITE);

        progressCallback.run();

        // Détermination du vainqueur
        if (blackCount > whiteCount)
            return GameResult.BLACK_WINS;
        else if (whiteCount > blackCount)
            return GameResult.WHITE_WINS;
        else
            return GameResult.TIE;
    }

    /**
     * Traite un tour de jeu pour le joueur courant
     * 
     * @return false si la partie est terminée, true sinon
     */
    private boolean processLocalMove() {
        // Vérifie si le joueur courant peut jouer
        if (!localBoard.hasValidMoves(localCurrentPlayer.getColor())) {
            // Vérifie si l'autre joueur peut jouer
            if (!localBoard.hasValidMoves(localCurrentPlayer.getColor().opposite())) {
                return false; // Fin de partie si aucun joueur ne peut jouer
            }
            return true; // Le joueur passe son tour
        }
        // Exécution du coup
        Move move = localCurrentPlayer.getMove(localBoard);
        if (move != null) {
            localBoard.makeMove(move);
        }
        return true;
    }
}

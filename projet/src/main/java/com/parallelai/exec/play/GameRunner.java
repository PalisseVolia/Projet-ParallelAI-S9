package com.parallelai.exec.play;

import java.util.concurrent.Callable;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;
import com.parallelai.game.Move;
import com.parallelai.game.Player;
import com.parallelai.models.utils.Model;
import com.parallelai.players.*;

public class GameRunner implements Callable<GameResult> {
    private final Model model1;
    private final Model model2;
    private final AIType aiType;
    private final Board localBoard;
    private Player localPlayer1;
    private Player localPlayer2;
    private Player localCurrentPlayer;
    private final Runnable progressCallback;

    public enum AIType { REGULAR, WEIGHTED }

    public GameRunner(Model model1, Model model2, AIType aiType, Runnable progressCallback) {
        this.model1 = model1;
        this.model2 = model2;
        this.aiType = aiType;
        this.localBoard = new Board();
        this.progressCallback = progressCallback;
        setupLocalPlayers();
    }

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

    @Override
    public GameResult call() {
        while (true) {
            if (!processLocalMove()) {
                break;
            }
            localCurrentPlayer = (localCurrentPlayer == localPlayer1) ? localPlayer2 : localPlayer1;
        }
        
        int blackCount = localBoard.getDiscCount(Disc.BLACK);
        int whiteCount = localBoard.getDiscCount(Disc.WHITE);
        
        progressCallback.run();
        
        if (blackCount > whiteCount) return GameResult.BLACK_WINS;
        else if (whiteCount > blackCount) return GameResult.WHITE_WINS;
        else return GameResult.TIE;
    }

    private boolean processLocalMove() {
        if (!localBoard.hasValidMoves(localCurrentPlayer.getColor())) {
            if (!localBoard.hasValidMoves(localCurrentPlayer.getColor().opposite())) {
                return false;
            }
            return true;
        }
        Move move = localCurrentPlayer.getMove(localBoard);
        if (move != null) {
            localBoard.makeMove(move);
        }
        return true;
    }
}

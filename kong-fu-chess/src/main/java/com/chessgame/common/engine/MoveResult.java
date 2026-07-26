package com.chessgame.common.engine;

import com.chessgame.common.model.Piece;
import com.chessgame.common.rules.MoveReason;

public final class MoveResult {
    private final boolean accepted;
    private final MoveReason reason;
    private final boolean capture;
    private final boolean gameOver;
    private final Piece.Color winner;

    private MoveResult(boolean accepted, MoveReason reason, boolean capture, boolean gameOver, Piece.Color winner) {
        this.accepted = accepted;
        this.reason = reason;
        this.capture = capture;
        this.gameOver = gameOver;
        this.winner = winner;
    }

    public static MoveResult accepted(boolean capture, boolean gameOver, Piece.Color winner) {
        return new MoveResult(true, MoveReason.OK, capture, gameOver, winner);
    }

    public static MoveResult rejected(MoveReason reason) {
        return new MoveResult(false, reason, false, false, null);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public MoveReason reason() {
        return reason;
    }

    public boolean isCapture() {
        return capture;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Piece.Color winner() {
        return winner;
    }
}

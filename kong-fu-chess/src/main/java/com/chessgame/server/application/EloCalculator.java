package com.chessgame.server.application;

import com.chessgame.common.model.Piece;

public final class EloCalculator {

    private static final int K_FACTOR = 32;

    public record Result(int newWhiteRating, int newBlackRating) {
    }

    private EloCalculator() {
    }

    public static Result compute(int whiteRating, int blackRating, Piece.Color winner) {
        double expectedWhite = 1.0 / (1.0 + Math.pow(10, (blackRating - whiteRating) / 400.0));
        double expectedBlack = 1.0 - expectedWhite;

        double scoreWhite = winner == null ? 0.5 : (winner == Piece.Color.WHITE ? 1.0 : 0.0);
        double scoreBlack = winner == null ? 0.5 : (winner == Piece.Color.BLACK ? 1.0 : 0.0);

        int newWhite = (int) Math.round(whiteRating + K_FACTOR * (scoreWhite - expectedWhite));
        int newBlack = (int) Math.round(blackRating + K_FACTOR * (scoreBlack - expectedBlack));

        return new Result(newWhite, newBlack);
    }
}

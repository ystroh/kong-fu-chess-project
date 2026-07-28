package com.chessgame.common.protocol.response;

import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

public record ActionOccurredMessage(
        String actionType,
        Piece.Color color,
        Position from,
        Position to,
        boolean capture,
        boolean gameOver,
        Piece.Color winner
) {
}

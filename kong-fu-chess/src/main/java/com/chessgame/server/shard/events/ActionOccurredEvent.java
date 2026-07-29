package com.chessgame.server.shard.events;

import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

public record ActionOccurredEvent(
        String actionType, Piece.Color color, Position from, Position to,
        boolean capture, boolean gameOver, Piece.Color winner) {
}

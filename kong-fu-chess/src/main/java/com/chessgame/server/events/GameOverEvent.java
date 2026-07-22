package com.chessgame.server.events;

import com.chessgame.common.model.Piece;

public record GameOverEvent(Piece.Color winner) {
}

package com.chessgame.common.protocol.response;

import com.chessgame.common.model.Piece;

public record ResumeMessage(Piece.Color color) {
}

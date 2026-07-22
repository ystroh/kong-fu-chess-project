package com.chessgame.common.protocol.response;

import com.chessgame.common.model.Piece;

public record RoleMessage(ParticipantRole role, Piece.Color color) {
}

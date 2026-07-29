package com.chessgame.server.bus;

import com.chessgame.common.model.Piece;

public record SessionAssignment(String gameId, Piece.Color color) {}
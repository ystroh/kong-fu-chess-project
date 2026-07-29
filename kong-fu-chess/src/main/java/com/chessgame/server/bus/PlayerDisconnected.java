package com.chessgame.server.bus;

import com.chessgame.common.model.Piece;

public record PlayerDisconnected(Piece.Color color) {}
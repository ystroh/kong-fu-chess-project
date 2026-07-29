package com.chessgame.server.common.bus;

import com.chessgame.common.model.Piece;

public record PlayerDisconnected(Piece.Color color) {}
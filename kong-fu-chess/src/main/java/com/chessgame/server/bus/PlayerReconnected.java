package com.chessgame.server.bus;

import com.chessgame.common.model.Piece;

public record PlayerReconnected(Piece.Color color) {}
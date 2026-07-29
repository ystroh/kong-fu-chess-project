package com.chessgame.server.bus;

import com.chessgame.common.model.Piece;

public record ReconnectInfo(String gameId, Piece.Color color) {}

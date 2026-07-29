package com.chessgame.server.common.bus;

import com.chessgame.common.model.Piece;

public record ReconnectInfo(String gameId, Piece.Color color) {}

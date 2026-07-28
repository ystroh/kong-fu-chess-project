package com.chessgame.server.bus;

import com.chessgame.common.model.Piece;

public record DisconnectNotice(String username, String gameId, Piece.Color color) {}

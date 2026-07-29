package com.chessgame.server.common.bus;

import com.chessgame.common.model.Piece;

public record DisconnectNotice(String username, String gameId, Piece.Color color) {}

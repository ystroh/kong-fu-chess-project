package com.chessgame.server.events;

import com.chessgame.common.model.Piece;

public record ReconnectEvent(Piece.Color reconnectedColor) {}
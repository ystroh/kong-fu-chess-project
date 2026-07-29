package com.chessgame.server.shard.events;

import com.chessgame.common.model.Piece;

public record ReconnectEvent(Piece.Color reconnectedColor) {}
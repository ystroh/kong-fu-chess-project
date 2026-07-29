package com.chessgame.server.events;

import com.chessgame.common.model.Piece;

public record DisconnectStatusEvent(Piece.Color disconnectedColor, int remainingSeconds) {}
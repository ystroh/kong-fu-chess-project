package com.chessgame.common.protocol.response;

import com.chessgame.common.engine.GameSnapshot;

public record GameStateMessage(GameSnapshot snapshot) {
}

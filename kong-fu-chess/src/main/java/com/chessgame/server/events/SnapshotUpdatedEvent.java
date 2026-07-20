package com.chessgame.server.events;

import com.chessgame.common.engine.GameSnapshot;

public record SnapshotUpdatedEvent(GameSnapshot snapshot) {
}
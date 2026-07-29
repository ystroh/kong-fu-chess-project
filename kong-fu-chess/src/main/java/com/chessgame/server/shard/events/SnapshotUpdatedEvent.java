package com.chessgame.server.shard.events;

import com.chessgame.common.engine.GameSnapshot;

public record SnapshotUpdatedEvent(GameSnapshot snapshot) {
}

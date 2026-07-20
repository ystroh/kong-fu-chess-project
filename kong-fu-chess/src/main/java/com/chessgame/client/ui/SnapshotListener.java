package com.chessgame.client.ui;

import com.chessgame.common.engine.GameSnapshot;

public interface SnapshotListener {
    void onSnapshotUpdated(GameSnapshot snapshot);
}
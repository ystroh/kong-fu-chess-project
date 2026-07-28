package com.chessgame.server.realtime.collision;

import com.chessgame.common.model.Board;
import com.chessgame.server.realtime.motion.MotionManager;

interface CollisionCandidate {
    long eventTime();

    boolean isStillRelevant(MotionManager motionManager, Board board);

    boolean resolve(Board board, MotionManager motionManager);
}

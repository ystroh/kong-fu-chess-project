package com.chessgame.common.engine;

import com.chessgame.common.model.Position;

public interface MoveChannel {
    MoveResult requestMove(Position from, Position to);
    MoveResult requestJump(Position at);
    GameSnapshot snapshot(Position selected);
}

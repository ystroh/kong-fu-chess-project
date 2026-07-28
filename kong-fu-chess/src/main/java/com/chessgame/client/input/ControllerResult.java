package com.chessgame.client.input;

import com.chessgame.common.engine.MoveResult;

public final class ControllerResult {
    private final MoveResult moveResult;

    private ControllerResult(MoveResult moveResult) {
        this.moveResult = moveResult;
    }

    public static ControllerResult noMove() {
        return new ControllerResult(null);
    }

    public static ControllerResult moveRequested(MoveResult moveResult) {
        return new ControllerResult(moveResult);
    }

    public boolean requestedMove() {
        return moveResult != null;
    }

    public MoveResult moveResult() {
        return moveResult;
    }
}

package com.chessgame.client.input;

import com.chessgame.client.ui.GameWindow;
import com.chessgame.client.ui.RemoteMoveSender;
import com.chessgame.common.engine.GameSnapshot;
import com.chessgame.common.engine.MoveChannel;
import com.chessgame.common.engine.MoveResult;
import com.chessgame.common.model.Position;

public final class RemoteMoveChannel implements MoveChannel {

    private final RemoteMoveSender moveSender;
    private final GameWindow gameWindow;

    public RemoteMoveChannel(RemoteMoveSender moveSender, GameWindow gameWindow) {
        this.moveSender = moveSender;
        this.gameWindow = gameWindow;
    }

    @Override
    public MoveResult requestMove(Position from, Position to) {
        moveSender.sendMove(from, to);
        return MoveResult.accepted();
    }

    @Override
    public MoveResult requestJump(Position at) {
        moveSender.sendJump(at);
        return MoveResult.accepted();
    }

    @Override
    public GameSnapshot snapshot(Position selected) {
        GameSnapshot latest = gameWindow.currentSnapshot();
        if (latest == null) {
            return null;
        }
        return new GameSnapshot(latest.width(), latest.height(), latest.pieces(), selected,
                latest.isGameOver(), latest.winner(), latest.moveHistory(), latest.scores());
    }
}
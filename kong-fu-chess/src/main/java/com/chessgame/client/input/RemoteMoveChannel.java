package com.chessgame.client.input;

import com.chessgame.client.network.ServerGateway;
import com.chessgame.client.ui.GameWindow;
import com.chessgame.common.engine.GameSnapshot;
import com.chessgame.common.engine.MoveChannel;
import com.chessgame.common.engine.MoveResult;
import com.chessgame.common.model.Position;

public final class RemoteMoveChannel implements MoveChannel {

    private final ServerGateway gateway;
    private final GameWindow gameWindow;

    public RemoteMoveChannel(ServerGateway gateway, GameWindow gameWindow) {
        this.gateway = gateway;
        this.gameWindow = gameWindow;
    }

    @Override
    public MoveResult requestMove(Position from, Position to) {
        gateway.sendMove(from, to);
        return MoveResult.accepted();
    }

    @Override
    public MoveResult requestJump(Position at) {
        gateway.sendJump(at);
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
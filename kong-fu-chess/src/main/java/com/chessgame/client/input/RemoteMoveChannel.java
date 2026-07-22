package com.chessgame.client.input;

import com.chessgame.client.network.ServerGateway;
import com.chessgame.client.ui.GameStateCoordinator;
import com.chessgame.common.engine.GameSnapshot;
import com.chessgame.common.engine.MoveChannel;
import com.chessgame.common.engine.MoveResult;
import com.chessgame.common.model.Position;

public final class RemoteMoveChannel implements MoveChannel {

    private final ServerGateway gateway;
    private final GameStateCoordinator coordinator;

    public RemoteMoveChannel(ServerGateway gateway, GameStateCoordinator coordinator) {
        this.gateway = gateway;
        this.coordinator = coordinator;
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
        GameSnapshot latest = coordinator.currentSnapshot();
        if (latest == null) {
            return null;
        }
        return new GameSnapshot(latest.width(), latest.height(), latest.pieces(), selected,
                latest.isGameOver(), latest.winner(), latest.moveHistory(), latest.scores());
    }
}
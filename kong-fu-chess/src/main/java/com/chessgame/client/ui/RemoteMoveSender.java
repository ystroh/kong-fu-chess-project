package com.chessgame.client.ui;

import com.chessgame.client.network.ServerConnection;
import com.chessgame.common.model.Position;

public final class RemoteMoveSender {

    private final ServerConnection connection;

    public RemoteMoveSender(ServerConnection connection) {
        this.connection = connection;
    }

    public void sendMove(Position from, Position to) {
        connection.send("MOVE " + toSquare(from) + toSquare(to));
    }

    public void sendJump(Position at) {
        connection.send("JUMP " + toSquare(at));
    }

    private String toSquare(Position p) {
        char file = (char) ('a' + p.col());
        int rank = 8 - p.row();
        return "" + file + rank;
    }
}
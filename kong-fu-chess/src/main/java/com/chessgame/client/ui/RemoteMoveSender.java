package com.chessgame.client.ui;

import com.chessgame.client.network.ServerConnection;
import com.chessgame.common.model.Position;
import com.chessgame.common.protocol.request.JumpMessage;
import com.chessgame.common.protocol.request.MessageType;
import com.chessgame.common.protocol.request.MoveMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public final class RemoteMoveSender {

    private final Gson gson = new Gson();
    private volatile ServerConnection connection;

    public RemoteMoveSender(ServerConnection connection) {
        this.connection = connection;
    }

    public void updateConnection(ServerConnection newConnection) {
        this.connection = newConnection;
    }

    public void sendMove(Position from, Position to) {
        String notation = toSquare(from) + toSquare(to);
        connection.send(toJson(MessageType.MOVE, new MoveMessage(notation)));
    }

    public void sendJump(Position at) {
        connection.send(toJson(MessageType.JUMP, new JumpMessage(toSquare(at))));
    }

    private String toJson(MessageType type, Object payload) {
        JsonObject json = gson.toJsonTree(payload).getAsJsonObject();
        json.addProperty("type", type.name());
        return json.toString();
    }

    private String toSquare(Position p) {
        char file = (char) ('a' + p.col());
        int rank = 8 - p.row();
        return "" + file + rank;
    }
}
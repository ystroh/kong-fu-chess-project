package com.chessgame.client.network;

import com.chessgame.common.model.Position;
import com.chessgame.common.protocol.request.CancelRoomMessage;
import com.chessgame.common.protocol.request.CreateRoomMessage;
import com.chessgame.common.protocol.request.JoinRoomMessage;
import com.chessgame.common.protocol.request.JumpMessage;
import com.chessgame.common.protocol.request.LoginMessage;
import com.chessgame.common.protocol.request.MessageType;
import com.chessgame.common.protocol.request.MoveMessage;
import com.chessgame.common.protocol.request.PlayMessage;
import com.chessgame.common.protocol.request.RegisterMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.client.logging.ClientLogger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ServerGateway {

    private record Registration(Object owner, Consumer<JsonObject> listener) {}

    private final Gson gson = new Gson();
    private final Map<ServerMessageType, List<Registration>> listeners = new EnumMap<>(ServerMessageType.class);

    private ServerConnection connection;
    private Runnable disconnectListener;

    public ServerGateway(ServerConnection connection) {
        this.connection = connection;
        wireConnection();
    }

    public void updateConnection(ServerConnection newConnection) {
        this.connection = newConnection;
        wireConnection();
    }

    public void setDisconnectListener(Runnable listener) {
        this.disconnectListener = listener;
        connection.setDisconnectListener(listener);
    }

    public void subscribe(Object owner, ServerMessageType type, Consumer<JsonObject> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(new Registration(owner, listener));
    }

    public void unsubscribeAll(Object owner) {
        for (List<Registration> forType : listeners.values()) {
            forType.removeIf(reg -> reg.owner() == owner);
        }
    }

    public void login(String username, String password) {
        send(MessageType.LOGIN, new LoginMessage(username, password));
    }

    public void register(String username, String password) {
        send(MessageType.REGISTER, new RegisterMessage(username, password));
    }

    public void play() {
        send(MessageType.PLAY, new PlayMessage());
    }

    public void createRoom(String roomName) {
        send(MessageType.CREATE_ROOM, new CreateRoomMessage(roomName));
    }

    public void joinRoom(String roomName) {
        send(MessageType.JOIN_ROOM, new JoinRoomMessage(roomName));
    }

    public void cancelRoom(String roomName) {
        send(MessageType.CANCEL_ROOM, new CancelRoomMessage(roomName));
    }

    public void sendMove(Position from, Position to) {
        send(MessageType.MOVE, new MoveMessage(toSquare(from) + toSquare(to)));
    }

    public void sendJump(Position at) {
        send(MessageType.JUMP, new JumpMessage(toSquare(at)));
    }

    private void wireConnection() {
        connection.setMessageListener(this::handleIncoming);
        if (disconnectListener != null) {
            connection.setDisconnectListener(disconnectListener);
        }
    }

    private void send(MessageType type, Object payload) {
        ClientLogger.log("sent type=" + type);
        JsonObject json = gson.toJsonTree(payload).getAsJsonObject();
        json.addProperty("type", type.name());
        connection.send(json.toString());
    }

    private void handleIncoming(String raw) {
        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
        ServerMessageType type = ServerMessageType.valueOf(json.get("type").getAsString());
        ClientLogger.log("received type=" + type);
        for (Registration reg : listeners.getOrDefault(type, List.of())) {
            reg.listener().accept(json);
        }
    }

    private String toSquare(Position p) {
        char file = (char) ('a' + p.col());
        int rank = 8 - p.row();
        return "" + file + rank;
    }
}

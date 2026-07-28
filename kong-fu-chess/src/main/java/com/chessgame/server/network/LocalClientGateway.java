package com.chessgame.server.network;

import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.server.ServerSocketConnection;
import com.chessgame.server.logging.ServerLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalClientGateway implements ClientGateway {

    private final Map<String, ServerSocketConnection> connections = new ConcurrentHashMap<>();

    public void register(String username, ServerSocketConnection connection) {
        connections.put(username, connection);
    }

    @Override
    public void sendTo(String username, ServerMessageType type, Object payload) {
        ServerSocketConnection connection = connections.get(username);
        if (connection != null) {
            ServerLogger.log("sent type=" + type + " to=" + username);
            connection.send(MessageSerializer.serialize(type, payload));
        } else {
            ServerLogger.log("sendTo failed, no connection for username=" + username);
        }
    }

    public void sendRaw(String username, String rawJson) {
        ServerSocketConnection connection = connections.get(username);
        if (connection != null) {
            connection.send(rawJson);
        }
    }
}

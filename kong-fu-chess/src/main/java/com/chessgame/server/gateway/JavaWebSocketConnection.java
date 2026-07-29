package com.chessgame.server.gateway;

import com.chessgame.server.logging.ServerLogger;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

public final class JavaWebSocketConnection implements ServerSocketConnection {

    private final WebSocket socket;

    public JavaWebSocketConnection(WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public synchronized void send(String message) {
        try {
            socket.send(message);
        } catch (WebsocketNotConnectedException e) {
            ServerLogger.log("send failed, connection already closed");
        }
    }
}

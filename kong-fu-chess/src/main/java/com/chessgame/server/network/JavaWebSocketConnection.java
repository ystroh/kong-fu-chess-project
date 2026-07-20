package com.chessgame.server.network;


import org.java_websocket.WebSocket;

public final class JavaWebSocketConnection implements ServerSocketConnection {

    private final WebSocket socket;

    public JavaWebSocketConnection(WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void send(String message) {
        socket.send(message);
    }
}
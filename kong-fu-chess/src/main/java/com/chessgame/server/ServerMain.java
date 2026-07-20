package com.chessgame.server;

import com.chessgame.server.network.ChessWebSocketServer;

public final class ServerMain {

    private static final int PORT = 8887;

    public static void main(String[] args) {
        ChessWebSocketServer server = new ChessWebSocketServer(PORT);
        server.start();
    }
}
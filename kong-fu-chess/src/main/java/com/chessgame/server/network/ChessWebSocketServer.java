package com.chessgame.server.network;

import com.chessgame.server.application.CommandHandler;
import com.chessgame.server.application.ServerSocketConnection;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public final class ChessWebSocketServer extends WebSocketServer {

    private final Map<WebSocket, ConnectionSession> sessions = new HashMap<>();
    private final CommandHandler commandHandler = new CommandHandler();

    public ChessWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        sessions.put(conn, new ConnectionSession());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ConnectionSession session = sessions.get(conn);
        if (session == null) {
            return;
        }
        ServerSocketConnection connection = new JavaWebSocketConnection(conn);
        commandHandler.handle(session, connection, message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        sessions.remove(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started");
    }
}
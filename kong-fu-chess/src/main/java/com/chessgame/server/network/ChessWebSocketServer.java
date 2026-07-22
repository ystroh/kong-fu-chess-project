package com.chessgame.server.network;

import com.chessgame.server.application.ServerSocketConnection;
import com.chessgame.server.repository.UserRepository;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.chessgame.server.application.CommandHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public final class ChessWebSocketServer extends WebSocketServer {

    private final CommandHandler commandHandler;
    private final Map<WebSocket, ConnectionSession> sessions = new HashMap<>();

    public ChessWebSocketServer(int port, UserRepository userRepository) {
        super(new InetSocketAddress(port));
        this.commandHandler = new CommandHandler(userRepository);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        ServerSocketConnection wrapped = new JavaWebSocketConnection(conn);
        sessions.put(conn, new ConnectionSession(wrapped));
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ConnectionSession session = sessions.get(conn);
        if (session == null) return;
        commandHandler.handle(session, message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ConnectionSession session = sessions.get(conn);
        if (session != null) {
            commandHandler.handleDisconnect(session);
        }
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
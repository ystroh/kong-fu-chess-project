package com.chessgame.server.network;

import com.chessgame.server.ConnectionSession;
import com.chessgame.server.JavaWebSocketConnection;
import com.chessgame.server.logging.ServerLogger;
import com.chessgame.server.repository.UserRepository;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public final class ChessWebSocketServer extends WebSocketServer {

    private final Map<WebSocket, ConnectionSession> sessions = new HashMap<>();
    private final CommandHandler commandHandler;

    public ChessWebSocketServer(int port, UserRepository userRepository) {
        super(new InetSocketAddress(port));
        ClientGateway gateway = new ClientGateway();
        this.commandHandler = new CommandHandler(userRepository, gateway);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        ServerLogger.log("connection opened");
        sessions.put(conn, new ConnectionSession(new JavaWebSocketConnection(conn)));
    }

    @Override
    public void onMessage(WebSocket conn, String rawMessage) {
        ServerLogger.log("received type=" + extractType(rawMessage));
        ConnectionSession session = sessions.get(conn);
        if (session == null) return;
        commandHandler.handle(session, rawMessage);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ConnectionSession session = sessions.get(conn);
        String username = session != null ? session.username() : "unknown";
        ServerLogger.log("connection closed, username=" + username + " code=" + code);
        if (session != null) {
            commandHandler.handleDisconnect(session);
        }
        sessions.remove(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ServerLogger.log("error: " + ex);
        ex.printStackTrace();
    }

    private String extractType(String rawMessage) {
        try {
            return JsonParser.parseString(rawMessage).getAsJsonObject().get("type").getAsString();
        } catch (Exception e) {
            return "unparsable";
        }
    }

    @Override
    public void onStart() {
        System.out.println("Server started");
    }
}

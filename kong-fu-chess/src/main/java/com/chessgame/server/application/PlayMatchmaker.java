package com.chessgame.server.application;

import com.chessgame.server.network.ConnectionSession;
import com.chessgame.server.PlayerConnection;
import com.chessgame.server.application.ServerSocketConnection;

public final class PlayMatchmaker {

    public record Pairing(
            ConnectionSession whiteSession, PlayerConnection white,
            ConnectionSession blackSession, PlayerConnection black) {}

    private ConnectionSession waitingSession;
    private ServerSocketConnection waitingConnection;

    public Pairing tryPair(ConnectionSession session, ServerSocketConnection connection) {
        if (waitingSession == null) {
            waitingSession = session;
            waitingConnection = connection;
            return null;
        }

        PlayerConnection white = new PlayerConnection(waitingConnection, PlayerConnection.Role.WHITE);
        PlayerConnection black = new PlayerConnection(connection, PlayerConnection.Role.BLACK);
        Pairing pairing = new Pairing(waitingSession, white, session, black);

        waitingSession = null;
        waitingConnection = null;
        return pairing;
    }
}
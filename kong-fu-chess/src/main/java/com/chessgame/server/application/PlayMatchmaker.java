package com.chessgame.server.application;

import com.chessgame.server.network.ConnectionSession;
import com.chessgame.server.PlayerConnection;

import java.util.ArrayList;
import java.util.List;

public final class PlayMatchmaker {

    private static final int RATING_RANGE = 100;

    public record Pairing(
            ConnectionSession whiteSession, PlayerConnection white,
            ConnectionSession blackSession, PlayerConnection black) {}

    private final List<ConnectionSession> waiting = new ArrayList<>();

    public synchronized Pairing tryPair(ConnectionSession session) {
        for (ConnectionSession candidate : waiting) {
            if (Math.abs(candidate.rating() - session.rating()) <= RATING_RANGE) {
                waiting.remove(candidate);
                PlayerConnection white = new PlayerConnection(candidate.connection(), PlayerConnection.Role.WHITE);
                PlayerConnection black = new PlayerConnection(session.connection(), PlayerConnection.Role.BLACK);
                return new Pairing(candidate, white, session, black);
            }
        }
        waiting.add(session);
        return null;
    }
}
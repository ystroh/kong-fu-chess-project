package com.chessgame.server.application;

import com.chessgame.server.ConnectionSession;
import com.chessgame.server.GameMatch;

import java.util.HashMap;
import java.util.Map;

public final class RoomManager {

    public enum CancelResult { CANCELLED, NOT_FOUND, NOT_AUTHORIZED }

    public sealed interface JoinResult permits NotFound, Paired, JoinedAsSpectator {}

    public record NotFound() implements JoinResult {}

    public record Paired(String whiteUsername, ConnectionSession whiteSession,
                         String blackUsername, ConnectionSession blackSession) implements JoinResult {}

    public record JoinedAsSpectator() implements JoinResult {}

    private static final class Room {
        String hostUsername;
        ConnectionSession hostSession;
        GameMatch match;
    }

    private final Map<String, Room> rooms = new HashMap<>();

    public boolean create(String roomName, ConnectionSession hostSession) {
        if (rooms.containsKey(roomName)) {
            return false;
        }
        Room room = new Room();
        room.hostUsername = hostSession.username();
        room.hostSession = hostSession;
        rooms.put(roomName, room);
        return true;
    }

    public JoinResult join(String roomName, ConnectionSession session) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return new NotFound();
        }

        if (room.match == null) {
            return new Paired(room.hostSession.username(), room.hostSession, session.username(), session);
        }

        room.match.addSpectator(session.username());
        return new JoinedAsSpectator();
    }

    public void attachMatch(String roomName, GameMatch match) {
        Room room = rooms.get(roomName);
        if (room != null) {
            room.match = match;
        }
    }

    public CancelResult cancel(String roomName, String requestingUsername) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return CancelResult.NOT_FOUND;
        }
        if (!room.hostUsername.equals(requestingUsername)) {
            return CancelResult.NOT_AUTHORIZED;
        }
        rooms.remove(roomName);
        return CancelResult.CANCELLED;
    }
}
package com.chessgame.server.application;

import com.chessgame.server.ConnectionSession;
import com.chessgame.server.GameMatch;

import java.util.HashMap;
import java.util.Map;

public final class RoomManager {

    public sealed interface JoinResult permits NotFound, Paired, JoinedAsSpectator {}

    public record NotFound() implements JoinResult {}

    public record Paired(String whiteUsername, ConnectionSession whiteSession,
                          String blackUsername, ConnectionSession blackSession) implements JoinResult {}

    public record JoinedAsSpectator() implements JoinResult {}

    private static final class Room {
        ConnectionSession hostSession;
        GameMatch match;
    }

    private final Map<String, Room> rooms = new HashMap<>();

    public void create(String roomName, ConnectionSession hostSession) {
        Room room = new Room();
        room.hostSession = hostSession;
        rooms.put(roomName, room);
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

    public void cancel(String roomName) {
        rooms.remove(roomName);
    }
}

package com.chessgame.server.application;

import com.chessgame.server.network.ConnectionSession;
import com.chessgame.server.GameMatch;
import com.chessgame.server.PlayerConnection;
import com.chessgame.server.application.ServerSocketConnection;

import java.util.HashMap;
import java.util.Map;

public final class RoomManager {

    public sealed interface JoinResult permits NotFound, Paired, JoinedAsSpectator {}
    public record NotFound() implements JoinResult {}
    public record Paired(ConnectionSession hostSession, PlayerConnection white, PlayerConnection black) implements JoinResult {}
    public record JoinedAsSpectator() implements JoinResult {}

    private static final class Room {
        ConnectionSession hostSession;
        ServerSocketConnection host;
        GameMatch match;
    }

    private final Map<String, Room> rooms = new HashMap<>();

    public void create(String roomName, ConnectionSession session, ServerSocketConnection connection) {
        Room room = new Room();
        room.hostSession = session;
        room.host = connection;
        rooms.put(roomName, room);
    }

    public JoinResult join(String roomName, ServerSocketConnection connection) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return new NotFound();
        }

        if (room.match == null) {
            PlayerConnection white = new PlayerConnection(room.host, PlayerConnection.Role.WHITE);
            PlayerConnection black = new PlayerConnection(connection, PlayerConnection.Role.BLACK);
            return new Paired(room.hostSession, white, black);
        }

        PlayerConnection spectator = new PlayerConnection(connection, PlayerConnection.Role.SPECTATOR);
        room.match.addSpectator(spectator);
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
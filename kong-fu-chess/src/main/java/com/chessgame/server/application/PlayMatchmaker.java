package com.chessgame.server.application;


import com.chessgame.server.PlayerConnection;
import com.chessgame.server.network.ServerSocketConnection;

public final class PlayMatchmaker {

    public record Pairing(PlayerConnection white, PlayerConnection black) {}

    private ServerSocketConnection waiting;

    public Pairing tryPair(ServerSocketConnection connection) {
        if (waiting == null) {
            waiting = connection;
            return null;
        }
        PlayerConnection white = new PlayerConnection(waiting, PlayerConnection.Role.WHITE);
        PlayerConnection black = new PlayerConnection(connection, PlayerConnection.Role.BLACK);
        waiting = null;
        return new Pairing(white, black);
    }
}

package com.chessgame.server;


import com.chessgame.common.model.Piece;
import com.chessgame.server.application.ServerSocketConnection;

public final class PlayerConnection {

    public enum Role { WHITE, BLACK, SPECTATOR }

    private final ServerSocketConnection connection;
    private final Role role;

    public PlayerConnection(ServerSocketConnection connection, Role role) {
        this.connection = connection;
        this.role = role;
    }

    public void send(String message) {
        connection.send(message);
    }

    public Role role() {
        return role;
    }

    public boolean isColor(Piece.Color color) {
        return (role == Role.WHITE && color == Piece.Color.WHITE)
                || (role == Role.BLACK && color == Piece.Color.BLACK);
    }
}

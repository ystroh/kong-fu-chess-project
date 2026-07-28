package com.chessgame.server;

import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.server.logging.ServerLogger;
import com.chessgame.server.network.MessageSerializer;

public final class ConnectionSession {

    public enum State { OPEN, AUTHENTICATED, IN_GAME }

    private final ServerSocketConnection connection;
    private State state = State.OPEN;
    private String username;
    private int rating = 1200;
    private Piece.Color color;
    private GameMatch match;

    public ConnectionSession(ServerSocketConnection connection) {
        this.connection = connection;
    }

    public ServerSocketConnection connection() {
        return connection;
    }

    public void send(ServerMessageType type, Object payload) {
        ServerLogger.log("sent type=" + type + " to=" + (username != null ? username : "unauthenticated"));
        connection.send(MessageSerializer.serialize(type, payload));
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String username() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int rating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Piece.Color color() {
        return color;
    }

    public void setColor(Piece.Color color) {
        this.color = color;
    }

    public GameMatch match() {
        return match;
    }

    public void setMatch(GameMatch match) {
        this.match = match;
    }
}

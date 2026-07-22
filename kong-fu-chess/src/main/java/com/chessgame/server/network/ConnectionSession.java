package com.chessgame.server.network;

import com.chessgame.server.GameMatch;
import com.chessgame.server.PlayerConnection;
import com.chessgame.server.application.ServerSocketConnection;


public final class ConnectionSession {

    public enum State { OPEN, AUTHENTICATED, IN_GAME }

    private final ServerSocketConnection connection;
    private State state = State.OPEN;
    private PlayerConnection playerConnection;
    private GameMatch match;

    private String username;
    private int rating = 1200;

    public int rating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public ConnectionSession(ServerSocketConnection connection) {
        this.connection = connection;
    }

    public ServerSocketConnection connection() { return connection; }
    public void send(String message) { connection.send(message); }

    public State state() { return state; }
    public void setState(State state) { this.state = state; }

    public String username() { return username; }
    public void setUsername(String username) { this.username = username; }

    public PlayerConnection playerConnection() { return playerConnection; }
    public void setPlayerConnection(PlayerConnection playerConnection) { this.playerConnection = playerConnection; }

    public GameMatch match() { return match; }
    public void setMatch(GameMatch match) { this.match = match; }
}

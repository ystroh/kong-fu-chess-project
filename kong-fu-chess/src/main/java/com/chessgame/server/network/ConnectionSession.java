package com.chessgame.server.network;

import com.chessgame.server.GameMatch;
import com.chessgame.server.PlayerConnection;

public final class ConnectionSession {

    public enum State { OPEN, AUTHENTICATED, IN_GAME }

    private State state = State.OPEN;
    private String username;
    private PlayerConnection playerConnection;
    private GameMatch match;

    public State state() { return state; }
    public void setState(State state) { this.state = state; }

    public String username() { return username; }
    public void setUsername(String username) { this.username = username; }

    public PlayerConnection playerConnection() { return playerConnection; }
    public void setPlayerConnection(PlayerConnection playerConnection) { this.playerConnection = playerConnection; }

    public GameMatch match() { return match; }
    public void setMatch(GameMatch match) { this.match = match; }
}
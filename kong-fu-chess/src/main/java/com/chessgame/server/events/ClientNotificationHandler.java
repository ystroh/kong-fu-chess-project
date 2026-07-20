package com.chessgame.server.events;

import com.chessgame.server.PlayerConnection;
import com.google.gson.Gson;

import java.util.List;

public final class ClientNotificationHandler {
    private final PlayerConnection white;
    private final PlayerConnection black;
    private final List<PlayerConnection> spectators;
    private final Gson gson = new Gson();

    public ClientNotificationHandler(PlayerConnection white, PlayerConnection black, List<PlayerConnection> spectators) {
        this.white = white;
        this.black = black;
        this.spectators = spectators;
    }

    public void onSnapshotUpdated(SnapshotUpdatedEvent event) {
        String json = gson.toJson(event.snapshot());
        white.send(json);
        black.send(json);
        for (PlayerConnection spectator : spectators) {
            spectator.send(json);
        }
    }
}
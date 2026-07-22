package com.chessgame.server.events;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

public final class LogHandler {
    private final PrintWriter writer;

    public LogHandler(String gameId) {
        PrintWriter w;
        try {
            w = new PrintWriter(new FileWriter("logs/game-" + gameId + ".log", true));
        } catch (IOException e) {
            w = new PrintWriter(System.out);
        }
        this.writer = w;
    }

    public void onSnapshotUpdated(SnapshotUpdatedEvent event) {
        writer.println(Instant.now() + " moves=" + event.snapshot().moveHistory().size());
        writer.flush();
    }

    public void onGameOver(GameOverEvent event) {
        writer.println(Instant.now() + " GAME OVER, winner=" + event.winner());
        writer.flush();
    }
}

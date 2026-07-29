package com.chessgame.server.shard;

import com.chessgame.common.engine.GameSnapshot;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;
import com.chessgame.server.common.Command;
import com.chessgame.server.shard.engine.GameEngine;
import com.chessgame.server.shard.events.ActionOccurredEvent;
import com.chessgame.server.shard.events.ClientNotificationHandler;
import com.chessgame.server.shard.events.DisconnectStatusEvent;
import com.chessgame.server.shard.events.EventBus;
import com.chessgame.server.shard.events.GameOverEvent;
import com.chessgame.server.shard.events.LogHandler;
import com.chessgame.server.shard.events.ReconnectEvent;
import com.chessgame.server.shard.events.SnapshotUpdatedEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class GameMatch {

    private static final int TICK_MS = 33;
    private static final int DISCONNECT_TIMEOUT_SECONDS = 20;

    private final String gameId;
    private final GameEngine engine;
    private final String whiteUsername;
    private final String blackUsername;
    private final List<String> spectatorUsernames = new ArrayList<>();
    private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();
    private final EventBus eventBus = new EventBus();
    private final ClientNotificationHandler notificationHandler;
    private final LogHandler logHandler;
    private final Map<Piece.Color, Instant> disconnectedSince = new EnumMap<>(Piece.Color.class);
    private final Map<Piece.Color, Integer> lastAnnouncedRemaining = new EnumMap<>(Piece.Color.class);

    private volatile boolean running = true;
    private boolean gameOverAnnounced = false;
    private Piece.Color resignedColor;
    private Thread consumerThread;

    public GameMatch(String gameId, GameEngine engine, String whiteUsername, String blackUsername, ClientGateway gateway) {
        this.gameId = gameId;
        this.engine = engine;
        this.whiteUsername = whiteUsername;
        this.blackUsername = blackUsername;
        this.notificationHandler = new ClientNotificationHandler(whiteUsername, blackUsername, spectatorUsernames, gateway);
        this.logHandler = new LogHandler(gameId);

        eventBus.subscribe(SnapshotUpdatedEvent.class, notificationHandler::onSnapshotUpdated);
        eventBus.subscribe(SnapshotUpdatedEvent.class, logHandler::onSnapshotUpdated);
        eventBus.subscribe(GameOverEvent.class, logHandler::onGameOver);
        eventBus.subscribe(ActionOccurredEvent.class, notificationHandler::onActionOccurred);
        eventBus.subscribe(DisconnectStatusEvent.class, notificationHandler::onDisconnectStatus);
        eventBus.subscribe(ReconnectEvent.class, notificationHandler::onReconnect);
    }

    public void start() {
        consumerThread = new Thread(this::runLoop, "GameMatch-" + gameId);
        consumerThread.start();
    }

    public void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    public void submitCommand(Command command) {
        queue.offer(command);
    }

    public void addSpectator(String username) {
        spectatorUsernames.add(username);
    }

    public void subscribeGameOver(Consumer<GameOverEvent> listener) {
        eventBus.subscribe(GameOverEvent.class, listener);
    }

    public void resign(Piece.Color color) {
        submitCommand(new Command.Resign(color));
    }

    public String opponentUsernameOf(Piece.Color color) {
        return color == Piece.Color.WHITE ? blackUsername : whiteUsername;
    }

    public void onPlayerDisconnected(Piece.Color color) {
        disconnectedSince.put(color, Instant.now());
        lastAnnouncedRemaining.put(color, DISCONNECT_TIMEOUT_SECONDS + 1);
    }

    public void onPlayerReconnected(Piece.Color color) {
        disconnectedSince.remove(color);
        lastAnnouncedRemaining.remove(color);
        eventBus.publish(new ReconnectEvent(color));
    }

    private void runLoop() {
        while (running) {
            try {
                Command command = queue.poll(TICK_MS, TimeUnit.MILLISECONDS);
                if (command != null) {
                    applyCommand(command);
                } else {
                    engine.wait(TICK_MS);
                }
                checkDisconnectTimeouts();
                broadcastSnapshot();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private void checkDisconnectTimeouts() {
        for (Map.Entry<Piece.Color, Instant> entry : disconnectedSince.entrySet()) {
            Piece.Color color = entry.getKey();
            long elapsedSeconds = Duration.between(entry.getValue(), Instant.now()).getSeconds();
            int remaining = (int) (DISCONNECT_TIMEOUT_SECONDS - elapsedSeconds);

            if (remaining <= 0) {
                resign(color);
                disconnectedSince.remove(color);
                lastAnnouncedRemaining.remove(color);
                return;
            }

            Integer lastAnnounced = lastAnnouncedRemaining.get(color);
            if (lastAnnounced == null || lastAnnounced != remaining) {
                lastAnnouncedRemaining.put(color, remaining);
                eventBus.publish(new DisconnectStatusEvent(color, remaining));
            }
        }
    }

    private void applyCommand(Command command) {
        if (resignedColor != null) {
            return;
        }

        if (command instanceof Command.Move move) {
            if (!ownsPieceAt(move.playerColor(), move.from())) {
                return;
            }
            var result = engine.requestMove(move.from(), move.to());
            if (result.isAccepted()) {
                eventBus.publish(new ActionOccurredEvent("MOVE", move.playerColor(), move.from(), move.to(),
                        result.isCapture(), result.isGameOver(), result.winner()));
            }
        } else if (command instanceof Command.Jump jump) {
            if (!ownsPieceAt(jump.playerColor(), jump.at())) {
                return;
            }
            var result = engine.requestJump(jump.at());
            if (result.isAccepted()) {
                eventBus.publish(new ActionOccurredEvent("JUMP", jump.playerColor(), jump.at(), jump.at(),
                        result.isCapture(), result.isGameOver(), result.winner()));
            }
        } else if (command instanceof Command.Resign resign) {
            resignedColor = resign.playerColor();
        }
    }

    private boolean ownsPieceAt(Piece.Color color, Position position) {
        for (GameSnapshot.PieceView piece : engine.snapshot(null).pieces()) {
            if (piece.position().equals(position)) {
                return piece.color() == color;
            }
        }
        return false;
    }

    private void broadcastSnapshot() {
        GameSnapshot raw = engine.snapshot(null);
        GameSnapshot snapshot = (resignedColor != null) ? withResignation(raw) : raw;

        eventBus.publish(new SnapshotUpdatedEvent(snapshot));

        if (snapshot.isGameOver() && !gameOverAnnounced) {
            gameOverAnnounced = true;
            eventBus.publish(new GameOverEvent(snapshot.winner()));
            stop();
        }
    }

    private GameSnapshot withResignation(GameSnapshot base) {
        Piece.Color winner = resignedColor == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
        return new GameSnapshot(base.width(), base.height(), base.pieces(), base.selectedCell(),
                true, winner, base.moveHistory(), base.scores());
    }
}
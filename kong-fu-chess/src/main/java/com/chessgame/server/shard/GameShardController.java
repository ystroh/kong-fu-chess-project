package com.chessgame.server.shard;

import com.chessgame.server.GameMatch;
import com.chessgame.server.application.MatchLauncher;
import com.chessgame.server.bus.CommandEnvelope;
import com.chessgame.server.bus.MatchAssignment;
import com.chessgame.server.bus.NatsEventBus;
import com.chessgame.server.bus.NatsSubjects;
import com.chessgame.server.bus.PlayerDisconnected;
import com.chessgame.server.bus.PlayerReconnected;

import java.util.concurrent.atomic.AtomicInteger;

public final class GameShardController {

    private final String shardId;
    private final MatchLauncher matchLauncher;
    private final NatsEventBus bus;
    private final AtomicInteger activeGameCount = new AtomicInteger(0);

    public GameShardController(String shardId, MatchLauncher matchLauncher, NatsEventBus bus) {
        this.shardId = shardId;
        this.matchLauncher = matchLauncher;
        this.bus = bus;
    }

    public void start() {
        bus.subscribe(NatsSubjects.shardAssign(shardId), MatchAssignment.class, this::onMatchAssigned);
    }

    public int activeGameCount() {
        return activeGameCount.get();
    }

    private void onMatchAssigned(MatchAssignment assignment) {
        GameMatch match = matchLauncher.launch(assignment.gameId(), assignment.whiteUsername(), assignment.blackUsername());
        activeGameCount.incrementAndGet();

        NatsEventBus.Subscription commandsSub = bus.subscribe(
                NatsSubjects.commands(assignment.gameId()), CommandEnvelope.class,
                envelope -> match.submitCommand(envelope.toCommand()));

        NatsEventBus.Subscription disconnectedSub = bus.subscribe(
                NatsSubjects.playerDisconnected(assignment.gameId()), PlayerDisconnected.class,
                event -> match.onPlayerDisconnected(event.color()));

        NatsEventBus.Subscription reconnectedSub = bus.subscribe(
                NatsSubjects.playerReconnected(assignment.gameId()), PlayerReconnected.class,
                event -> match.onPlayerReconnected(event.color()));

        match.subscribeGameOver(event -> {
            commandsSub.unsubscribe();
            disconnectedSub.unsubscribe();
            reconnectedSub.unsubscribe();
            activeGameCount.decrementAndGet();
        });
    }
}
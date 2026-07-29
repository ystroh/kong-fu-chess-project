package com.chessgame.server.shard;

import com.chessgame.server.shard.application.MatchLauncher;
import com.chessgame.server.common.bus.CommandEnvelope;
import com.chessgame.server.common.bus.GameEnded;
import com.chessgame.server.common.bus.MatchAssignment;
import com.chessgame.server.common.bus.NatsEventBus;
import com.chessgame.server.common.bus.NatsSubjects;
import com.chessgame.server.common.bus.PlayerDisconnected;
import com.chessgame.server.common.bus.PlayerReconnected;
import com.chessgame.server.common.bus.SpectatorJoinRequest;

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

        NatsEventBus.Subscription spectatorJoinSub = bus.subscribe(
                NatsSubjects.spectatorJoin(assignment.gameId()), SpectatorJoinRequest.class,
                event -> match.addSpectator(event.username()));

        match.subscribeGameOver(event -> {
            commandsSub.unsubscribe();
            disconnectedSub.unsubscribe();
            reconnectedSub.unsubscribe();
            spectatorJoinSub.unsubscribe();
            activeGameCount.decrementAndGet();
            bus.publish(NatsSubjects.gameEnded(assignment.gameId()), new GameEnded(assignment.gameId()));
        });
    }
}
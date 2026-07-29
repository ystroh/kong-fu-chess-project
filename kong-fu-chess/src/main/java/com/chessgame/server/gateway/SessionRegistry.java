package com.chessgame.server.gateway;

import com.chessgame.server.common.bus.NatsEventBus;
import com.chessgame.server.common.bus.NatsSubjects;
import com.chessgame.server.common.bus.SessionAssignment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;



public final class SessionRegistry {

    private final Map<String, ConnectionSession> sessionsByUsername = new ConcurrentHashMap<>();
    private final Map<String, List<NatsEventBus.Subscription>> subscriptionsByUsername = new ConcurrentHashMap<>();
    private final NatsEventBus bus;

    public SessionRegistry(NatsEventBus bus) {
        this.bus = bus;
    }

    public void register(String username, ConnectionSession session) {
        sessionsByUsername.put(username, session);

        List<NatsEventBus.Subscription> previous = subscriptionsByUsername.remove(username);
        if (previous != null) {
            for (NatsEventBus.Subscription subscription : previous) {
                subscription.unsubscribe();
            }
        }

        NatsEventBus.Subscription outboxSub = bus.subscribe(NatsSubjects.clientOutbox(username), String.class,
                rawJson -> session.connection().send(rawJson));

        NatsEventBus.Subscription assignmentSub = bus.subscribe(NatsSubjects.sessionAssigned(username), SessionAssignment.class,
                assignment -> {
                    session.setGameId(assignment.gameId());
                    session.setColor(assignment.color());
                    session.setState(ConnectionSession.State.IN_GAME);
                });

        subscriptionsByUsername.put(username, List.of(outboxSub, assignmentSub));
    }
}
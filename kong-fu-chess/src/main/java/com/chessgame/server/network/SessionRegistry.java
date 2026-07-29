package com.chessgame.server.network;

import com.chessgame.server.ConnectionSession;
import com.chessgame.server.bus.NatsEventBus;
import com.chessgame.server.bus.NatsSubjects;
import com.chessgame.server.bus.SessionAssignment;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionRegistry {

    private final Map<String, ConnectionSession> sessionsByUsername = new ConcurrentHashMap<>();
    private final Set<String> subscribedUsernames = ConcurrentHashMap.newKeySet();
    private final NatsEventBus bus;

    public SessionRegistry(NatsEventBus bus) {
        this.bus = bus;
    }

    public void register(String username, ConnectionSession session) {
        sessionsByUsername.put(username, session);
        if (subscribedUsernames.add(username)) {
            bus.subscribe(NatsSubjects.clientOutbox(username), String.class,
                    rawJson -> session.connection().send(rawJson));
            bus.subscribe(NatsSubjects.sessionAssigned(username), SessionAssignment.class,
                    assignment -> {
                        session.setGameId(assignment.gameId());
                        session.setColor(assignment.color());
                        session.setState(ConnectionSession.State.IN_GAME);
                    });
        }
    }
}
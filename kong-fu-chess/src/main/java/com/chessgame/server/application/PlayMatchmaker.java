package com.chessgame.server.application;

import com.chessgame.server.ConnectionSession;

import java.util.ArrayList;
import java.util.List;

public final class PlayMatchmaker {

    private static final int RATING_RANGE = 100;

    public sealed interface PairResult permits Waiting, Paired {}

    public record Waiting() implements PairResult {}

    public record Paired(
            String whiteUsername, ConnectionSession whiteSession,
            String blackUsername, ConnectionSession blackSession) implements PairResult {}

    private final List<ConnectionSession> waiting = new ArrayList<>();

    public synchronized PairResult tryPair(ConnectionSession session) {
        for (ConnectionSession candidate : waiting) {
            if (Math.abs(candidate.rating() - session.rating()) <= RATING_RANGE) {
                waiting.remove(candidate);
                return new Paired(candidate.username(), candidate, session.username(), session);
            }
        }
        waiting.add(session);
        return new Waiting();
    }
}

package com.chessgame.server.application;

import com.chessgame.server.redis.RedisClient;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;

public final class PlayMatchmaker {

    private static final String KEY = "play:waiting";
    private static final int RATING_RANGE = 100;

    public sealed interface PairResult permits Waiting, Paired {}

    public record Waiting() implements PairResult {}

    public record Paired(String whiteUsername, String blackUsername) implements PairResult {}

    public PairResult tryPair(String username, int rating) {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            List<String> candidates = jedis.zrangeByScore(KEY, rating - RATING_RANGE, rating + RATING_RANGE);

            for (String candidate : candidates) {
                if (candidate.equals(username)) {
                    continue;
                }
                long removed = jedis.zrem(KEY, candidate);
                if (removed == 1) {
                    return new Paired(candidate, username);
                }
            }

            jedis.zadd(KEY, rating, username);
            return new Waiting();
        }
    }
}
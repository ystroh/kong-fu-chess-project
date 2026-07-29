package com.chessgame.server.matchmaker;

import com.chessgame.server.redis.RedisClient;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
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
                    jedis.del(waitingSinceKey(candidate));
                    return new Paired(candidate, username);
                }
            }

            jedis.zadd(KEY, rating, username);
            jedis.set(waitingSinceKey(username), String.valueOf(System.currentTimeMillis()));
            return new Waiting();
        }
    }

    public void cancel(String username) {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            jedis.zrem(KEY, username);
            jedis.del(waitingSinceKey(username));
        }
    }

    public List<String> removeExpiredWaiters(long timeoutMs) {
        List<String> expired = new ArrayList<>();
        try (Jedis jedis = RedisClient.pool().getResource()) {
            List<String> waiting = jedis.zrange(KEY, 0, -1);
            long now = System.currentTimeMillis();

            for (String username : waiting) {
                String sinceStr = jedis.get(waitingSinceKey(username));
                if (sinceStr == null || now - Long.parseLong(sinceStr) >= timeoutMs) {
                    jedis.zrem(KEY, username);
                    jedis.del(waitingSinceKey(username));
                    expired.add(username);
                }
            }
        }
        return expired;
    }

    private static String waitingSinceKey(String username) {
        return "play:waiting-since:" + username;
    }
}
package com.chessgame.server.matchmaker;

import com.chessgame.server.redis.RedisClient;
import redis.clients.jedis.Jedis;

public final class RoomManager {

    private static final String ROOM_KEY_PREFIX = "room:";

    public sealed interface JoinResult permits NotFound, Paired, JoinedAsSpectator {}

    public record NotFound() implements JoinResult {}

    public record Paired(String hostUsername, String joinerUsername) implements JoinResult {}

    public record JoinedAsSpectator() implements JoinResult {}

    public boolean create(String roomName, String hostUsername) {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            String key = ROOM_KEY_PREFIX + roomName;
            long fieldsSet = jedis.hsetnx(key, "host", hostUsername);
            return fieldsSet == 1;
        }
    }

    public JoinResult join(String roomName, String username) {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            String key = ROOM_KEY_PREFIX + roomName;
            String host = jedis.hget(key, "host");
            if (host == null) {
                return new NotFound();
            }

            boolean alreadyMatched = jedis.hexists(key, "matched");
            if (!alreadyMatched) {
                jedis.hset(key, "matched", "true");
                return new Paired(host, username);
            }

            return new JoinedAsSpectator();
        }
    }

    public void cancel(String roomName) {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            jedis.del(ROOM_KEY_PREFIX + roomName);
        }
    }
}
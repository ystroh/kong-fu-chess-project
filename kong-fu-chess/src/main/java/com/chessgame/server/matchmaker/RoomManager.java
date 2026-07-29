package com.chessgame.server.matchmaker;

import com.chessgame.server.redis.RedisClient;
import redis.clients.jedis.Jedis;

public final class RoomManager {

    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String HOST_INDEX_PREFIX = "room-host:";

    public sealed interface JoinResult permits NotFound, Paired, JoinedAsSpectator {}

    public record NotFound() implements JoinResult {}

    public record Paired(String hostUsername, String joinerUsername) implements JoinResult {}

    public record JoinedAsSpectator(String gameId) implements JoinResult {}

    public boolean create(String roomName, String hostUsername) {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            String key = ROOM_KEY_PREFIX + roomName;
            long fieldsSet = jedis.hsetnx(key, "host", hostUsername);
            if (fieldsSet == 1) {
                jedis.set(HOST_INDEX_PREFIX + hostUsername, roomName);
            }
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

            String gameId = jedis.hget(key, "gameId");
            return new JoinedAsSpectator(gameId);
        }
    }

    public void onGameCreated(String whiteUsername, String gameId) {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            String hostIndexKey = HOST_INDEX_PREFIX + whiteUsername;
            String roomName = jedis.get(hostIndexKey);
            if (roomName != null) {
                jedis.hset(ROOM_KEY_PREFIX + roomName, "gameId", gameId);
                jedis.del(hostIndexKey);
            }
        }
    }

    public void cancel(String roomName) {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            String key = ROOM_KEY_PREFIX + roomName;
            String host = jedis.hget(key, "host");
            if (host != null) {
                jedis.del(HOST_INDEX_PREFIX + host);
            }
            jedis.del(key);
        }
    }
}
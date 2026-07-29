package com.chessgame.server.allocator;

import com.chessgame.server.redis.RedisClient;
import redis.clients.jedis.Jedis;

public final class ShardCapacityRegistry {

    private static final String LOAD_KEY = "shard:load";

    private final ShardDiscovery shardDiscovery;
    private final int maxGamesPerShard;

    public ShardCapacityRegistry(ShardDiscovery shardDiscovery) {
        this.shardDiscovery = shardDiscovery;
        this.maxGamesPerShard = Integer.parseInt(System.getenv().getOrDefault("SHARD_MAX_GAMES", "200"));
    }

    public String pickShardForNewGame() {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            for (String shardId : shardDiscovery.listAvailableShardIds()) {
                String value = jedis.hget(LOAD_KEY, shardId);
                int load = value != null ? Integer.parseInt(value) : 0;
                if (load < maxGamesPerShard) {
                    jedis.hincrBy(LOAD_KEY, shardId, 1);
                    return shardId;
                }
            }
            return null;
        }
    }

    public void onGameEnded(String shardId) {
        try (Jedis jedis = RedisClient.pool().getResource()) {
            jedis.hincrBy(LOAD_KEY, shardId, -1);
        }
    }
}
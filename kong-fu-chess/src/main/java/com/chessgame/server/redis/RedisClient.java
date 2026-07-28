package com.chessgame.server.redis;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class RedisClient {

    private static final JedisPool pool = createPool();

    private RedisClient() {
    }

    private static JedisPool createPool() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        return new JedisPool(new JedisPoolConfig(), host, port);
    }

    public static JedisPool pool() {
        return pool;
    }
}

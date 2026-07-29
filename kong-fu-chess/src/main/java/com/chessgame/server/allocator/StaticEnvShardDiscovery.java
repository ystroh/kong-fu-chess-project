package com.chessgame.server.allocator;

import java.util.Arrays;
import java.util.List;

public final class StaticEnvShardDiscovery implements ShardDiscovery {

    private final List<String> shardIds;

    public StaticEnvShardDiscovery() {
        this.shardIds = Arrays.asList(System.getenv().getOrDefault("SHARD_IDS", "shard-1").split(","));
    }

    @Override
    public List<String> listAvailableShardIds() {
        return shardIds;
    }
}
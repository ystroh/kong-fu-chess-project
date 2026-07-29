package com.chessgame.server.allocator;

import java.util.List;

public interface ShardDiscovery {
    List<String> listAvailableShardIds();
}
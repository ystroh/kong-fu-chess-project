package com.chessgame.server.allocator;
import com.chessgame.server.bus.NatsEventBus;

public final class GameAllocatorMain {
    public static void main(String[] args) {
        NatsEventBus bus = new NatsEventBus();
        ShardDiscovery shardDiscovery = new StaticEnvShardDiscovery();
        ShardCapacityRegistry shardCapacityRegistry = new ShardCapacityRegistry(shardDiscovery);
        GameAllocatorController controller = new GameAllocatorController(shardCapacityRegistry, bus);
        controller.start();
    }
}
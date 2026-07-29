package com.chessgame.server.shard;

import com.chessgame.server.application.MatchLauncher;
import com.chessgame.server.bus.NatsEventBus;
import com.chessgame.server.network.ClientGateway;
import com.chessgame.server.network.NatsClientGateway;
import com.chessgame.server.repository.Database;
import com.chessgame.server.repository.UserRepository;

public final class GameShardMain {

    public static void main(String[] args) {
        String shardId = System.getenv().getOrDefault("SHARD_ID", "shard-unknown");

        NatsEventBus bus = new NatsEventBus();
        UserRepository userRepository = new UserRepository(new Database());
        ClientGateway gateway = new NatsClientGateway(bus);
        MatchLauncher matchLauncher = new MatchLauncher(gateway, userRepository);

        GameShardController controller = new GameShardController(shardId, matchLauncher, bus);
        controller.start();
    }
}
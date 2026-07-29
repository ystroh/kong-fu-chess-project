package com.chessgame.server.gateway;

import com.chessgame.server.common.bus.NatsEventBus;
import com.chessgame.server.repository.Database;
import com.chessgame.server.repository.UserRepository;

public final class ServerMain {

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8887"));

        NatsEventBus bus = new NatsEventBus();
        Database database = new Database();
        UserRepository userRepository = new UserRepository(database);

        ChessWebSocketServer server = new ChessWebSocketServer(port, userRepository, bus);
        server.start();
    }
}
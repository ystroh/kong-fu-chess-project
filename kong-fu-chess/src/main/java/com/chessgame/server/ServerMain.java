package com.chessgame.server;

import com.chessgame.server.network.ChessWebSocketServer;
import com.chessgame.server.repository.Database;
import com.chessgame.server.repository.UserRepository;

public final class ServerMain {

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8887"));

        Database database = new Database();
        UserRepository userRepository = new UserRepository(database);

        ChessWebSocketServer server = new ChessWebSocketServer(port, userRepository);
        server.start();
    }
}
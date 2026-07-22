package com.chessgame.server;

import com.chessgame.server.network.ChessWebSocketServer;
import com.chessgame.server.repository.Database;
import com.chessgame.server.repository.UserRepository;

public final class ServerMain {

    private static final int PORT = 8887;

    public static void main(String[] args) {
        Database database = new Database();
        UserRepository userRepository = new UserRepository(database);

        ChessWebSocketServer server = new ChessWebSocketServer(PORT, userRepository);
        server.start();
    }
}

package com.chessgame.server.application;

import com.chessgame.server.GameMatch;
import com.chessgame.server.engine.GameEngine;
import com.chessgame.server.engine.GameFactory;
import com.chessgame.server.events.ScoreHandler;
import com.chessgame.server.network.ClientGateway;
import com.chessgame.server.repository.UserRepository;

public final class MatchLauncher {

    private final ClientGateway gateway;
    private final UserRepository userRepository;

    public MatchLauncher(ClientGateway gateway, UserRepository userRepository) {
        this.gateway = gateway;
        this.userRepository = userRepository;
    }

    public GameMatch launch(String gameId, String whiteUsername, String blackUsername) {
        GameEngine engine = GameFactory.newStandardGame();
        GameMatch match = new GameMatch(gameId, engine, whiteUsername, blackUsername, gateway);

        ScoreHandler scoreHandler = new ScoreHandler(whiteUsername, blackUsername, userRepository);
        match.subscribeGameOver(scoreHandler::onGameOver);

        match.start();
        return match;
    }
}

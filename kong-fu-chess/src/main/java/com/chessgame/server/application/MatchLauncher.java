package com.chessgame.server.application;

import com.chessgame.server.engine.GameFactory;
import com.chessgame.server.GameMatch;
import com.chessgame.server.PlayerConnection;
import com.chessgame.server.engine.GameEngine;
import com.chessgame.server.events.ScoreHandler;
import com.chessgame.server.repository.UserRepository;

public final class MatchLauncher {

    private final UserRepository userRepository;

    public MatchLauncher(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public GameMatch launch(String gameId, PlayerConnection white, PlayerConnection black,
                            String whiteUsername, String blackUsername) {
        GameEngine engine = GameFactory.newStandardGame();
        GameMatch match = new GameMatch(gameId, engine, white, black);

        ScoreHandler scoreHandler = new ScoreHandler(whiteUsername, blackUsername, userRepository);
        match.subscribeGameOver(scoreHandler::onGameOver);

        match.start();
        return match;
    }
}
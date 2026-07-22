package com.chessgame.server.events;

import com.chessgame.server.application.EloCalculator;
import com.chessgame.server.repository.UserRepository;

public final class ScoreHandler {

    private final String whiteUsername;
    private final String blackUsername;
    private final UserRepository userRepository;

    public ScoreHandler(String whiteUsername, String blackUsername, UserRepository userRepository) {
        this.whiteUsername = whiteUsername;
        this.blackUsername = blackUsername;
        this.userRepository = userRepository;
    }

    public void onGameOver(GameOverEvent event) {
        int whiteRating = userRepository.findByUsername(whiteUsername)
                .map(UserRepository.User::rating).orElse(1200);
        int blackRating = userRepository.findByUsername(blackUsername)
                .map(UserRepository.User::rating).orElse(1200);

        EloCalculator.Result result = EloCalculator.compute(whiteRating, blackRating, event.winner());

        userRepository.updateRating(whiteUsername, result.newWhiteRating());
        userRepository.updateRating(blackUsername, result.newBlackRating());
    }
}

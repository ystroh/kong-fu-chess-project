
package com.chessgame.server.application;

import com.chessgame.server.GameFactory;
import com.chessgame.server.GameMatch;
import com.chessgame.server.PlayerConnection;
import com.chessgame.server.engine.GameEngine;

public final class MatchLauncher {

    public GameMatch launch(String gameId, PlayerConnection white, PlayerConnection black) {
        GameEngine engine = GameFactory.newStandardGame();
        GameMatch match = new GameMatch(gameId, engine, white, black);
        match.start();
        return match;
    }
}
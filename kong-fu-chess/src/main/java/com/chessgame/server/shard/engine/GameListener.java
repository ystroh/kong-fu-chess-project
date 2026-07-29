package com.chessgame.server.shard.engine;


public interface GameListener {
    void onGameStateChanged(GameEngine engine);
}

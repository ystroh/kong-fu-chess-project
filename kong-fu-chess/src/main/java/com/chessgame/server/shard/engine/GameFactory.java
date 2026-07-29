package com.chessgame.server.shard.engine;

import com.chessgame.common.model.Board;
import com.chessgame.server.shard.io.BoardParser;
import com.chessgame.server.shard.model.GameState;
import com.chessgame.server.shard.realtime.RealTimeArbiter;
import com.chessgame.server.shard.rules.PieceRules;
import com.chessgame.server.shard.rules.RuleEngine;

public final class GameFactory {

    private static final String STANDARD_POSITION =
            "bR bN bB bQ bK bB bN bR\n" +
            "bP bP bP bP bP bP bP bP\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            "wP wP wP wP wP wP wP wP\n" +
            "wR wN wB wQ wK wB wN wR";

    private GameFactory() {
    }

    public static GameEngine newStandardGame() {
        Board board = new BoardParser().parse(STANDARD_POSITION);
        GameState gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        return new GameEngine(board, gameState, ruleEngine, arbiter);
    }
}

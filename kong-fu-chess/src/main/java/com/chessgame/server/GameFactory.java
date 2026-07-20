package com.chessgame.server;


import com.chessgame.common.model.Board;
import com.chessgame.server.engine.GameEngine;
import com.chessgame.server.io.BoardParser;
import com.chessgame.server.model.GameState;
import com.chessgame.server.realtime.RealTimeArbiter;
import com.chessgame.server.rules.PieceRules;
import com.chessgame.server.rules.RuleEngine;

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

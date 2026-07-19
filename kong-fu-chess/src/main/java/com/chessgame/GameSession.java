package com.chessgame;

import com.chessgame.server.engine.GameEngine;
import com.chessgame.client.input.BoardMapper;
import com.chessgame.client.input.Controller;
import com.chessgame.common.model.Board;
import com.chessgame.server.model.GameState;
import com.chessgame.server.realtime.RealTimeArbiter;
import com.chessgame.server.rules.PieceRules;
import com.chessgame.server.rules.RuleEngine;

public final class GameSession {
    public final Board board;
    public final GameEngine gameEngine;
    public final Controller controller;

    public GameSession(Board board) {
        this.board = board;

        GameState gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        this.gameEngine = new GameEngine(board, gameState, ruleEngine, arbiter);
        this.controller = new Controller(new BoardMapper(board), gameEngine);
    }
}

package com.chessgame;

import com.chessgame.engine.GameEngine;
import com.chessgame.input.BoardMapper;
import com.chessgame.input.Controller;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;

/**
 * GameSession / מפגש-משחק
 *
 * תפקיד: "שורש-ההרכבה" (composition root) - בהינתן Board מוכן, בונה
 * ומחבר פעם אחת את כל שכבות המשחק (RuleEngine, RealTimeArbiter,
 * GameEngine, Controller). זו האחריות היחידה שלה - "מי בונה מה
 * ומעביר למי" - לא קריאת-קלט, לא פרסינג-פקודות.
 *
 * public (לא package-private) כי גם ScriptRunner (בחבילה texttests)
 * צריך להשתמש בה - כדי לא לשכפל את לוגיקת-ההרכבה פעמיים (פעם ב-App,
 * פעם ב-ScriptRunner), וכדי ש-ScriptRunner לא יצטרך לייבא בעצמו
 * RuleEngine/RealTimeArbiter/PieceRules/GameState - בדיוק כמו
 * שהמסמך מגדיר את התלויות המצומצמות של TextTestRunner.
 */
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

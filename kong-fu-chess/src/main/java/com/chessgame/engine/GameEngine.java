package com.chessgame.engine;

import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Position;
import com.chessgame.rules.MoveReason;
import com.chessgame.rules.MoveValidation;
import com.chessgame.rules.RuleEngine;
import com.chessgame.realtime.RealTimeArbiter;

/**
 * GameEngine / מנוע המשחק
 *
 * תפקיד: מתאם שירות-אפליקציה - גבול-הפקודה הציבורי היחיד שדרכו
 * Controller ו-TextTestRunner מדברים עם המערכת. שלוש פקודות
 * ציבוריות: requestMove (מהמסמך), wait (מהמסמך), requestJump
 * (תוספת שלנו, לפי אותו תבנית בדיוק).
 *
 * GameEngine לא מכיל שום לוגיקת-תנועה ספציפית-לכלי, מיפוי פיקסלים,
 * רינדור, פרסינג טקסט, או לוגיקת test-runner - הוא רק מתאם קריאות
 * בין RuleEngine (חוקיות שחמט) ל-RealTimeArbiter (מכניקת זמן-אמת),
 * ומחזיק את GameState (game_over).
 *
 * סדר הבדיקות ב-requestMove, בהתאם למסמך: game_over -> "עסוק כבר"
 * -> RuleEngine.validateMove -> בדיקת-התנגשות נוספת -> התחלת תנועה.
 * "עסוק" ו"התנגשות" הם המקבילה שלנו ל-motion_in_progress - לא "יש
 * תנועה כלשהי במערכת" (מדיניות המסמך המקורית), אלא "הכלי הזה עסוק"
 * או "המסלול/היעד מתנגש עם תנועה קיימת" - כך אפשר כמה תנועות
 * בו-זמנית בלי לוותר על ההגנה מפני בקשות סותרות.
 */
public final class GameEngine {
    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter realTimeArbiter;

    public GameEngine(Board board, GameState gameState, RuleEngine ruleEngine, RealTimeArbiter realTimeArbiter) {
        this.board = board;
        this.gameState = gameState;
        this.ruleEngine = ruleEngine;
        this.realTimeArbiter = realTimeArbiter;
    }

    public MoveResult requestMove(Position source, Position destination) {
        if (gameState.isGameOver()) {
            return MoveResult.rejected(MoveReason.GAME_OVER);
        }
        if (realTimeArbiter.isPieceBusy(source)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }

        MoveValidation validation = ruleEngine.validateMove(source, destination);
        if (!validation.isValid()) {
            return MoveResult.rejected(validation.reason());
        }

        if (!realTimeArbiter.canStartMotion(source, destination)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }

        realTimeArbiter.startMotion(source, destination);
        return MoveResult.accepted();
    }

    /**
     * מבקש קפיצה עבור הכלי בתא הנתון (תוספת שלנו - אין חוק-שחמט
     * לבדוק, רק שהכלי קיים והוא לא כבר עסוק).
     */
    public MoveResult requestJump(Position position) {
        if (gameState.isGameOver()) {
            return MoveResult.rejected(MoveReason.GAME_OVER);
        }
        if (realTimeArbiter.isPieceBusy(position)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }
        if (board.pieceAt(position) == null) {
            return MoveResult.rejected(MoveReason.EMPTY_SOURCE);
        }

        realTimeArbiter.startJump(position);
        return MoveResult.accepted();
    }

    public void wait(int milliseconds) {
        boolean kingCaptured = realTimeArbiter.advanceTime(milliseconds);
        if (kingCaptured) {
            gameState.setGameOver(true);
        }
    }

    public GameSnapshot snapshot() {
        return new GameSnapshot(board, gameState.isGameOver());
    }
}

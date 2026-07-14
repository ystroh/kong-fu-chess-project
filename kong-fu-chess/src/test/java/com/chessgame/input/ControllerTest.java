package com.chessgame.input;

import com.chessgame.engine.GameEngine;
import com.chessgame.engine.MoveResult;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.io.BoardParser;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ControllerTest / טסטים ל-Controller
 *
 * הערה חשובה: המסמך מציע "unit tests עם GameEngine מזויף (fake)".
 * אצלנו GameEngine היא מחלקה final (בכוונה - אין סיבה שמישהו יירש
 * ממנה) - כדי "לזייף" אותה באמת היינו צריכות ספריית-mocking נוספת
 * (כמו Mockito) שעדיין לא הוספנו לפרויקט. לכן, בפרגמטיות, הטסטים
 * כאן משתמשים ב-GameEngine *אמיתי* (עם לוח קטן וממוקד) - זה בודק
 * את התנהגות ה-Controller בפועל, רק לא ב"בידוד מוחלט" כמו שהמסמך
 * מציע. אם בעתיד תרצי mocking אמיתי - נוסיף Mockito.
 */
class ControllerTest {

    private Board board;
    private Controller controller;

    @BeforeEach
    void setUp() {
        board = new BoardParser().parse("wK bR .\n. . .\n. . .");
        GameState gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        GameEngine engine = new GameEngine(board, gameState, ruleEngine, arbiter);
        BoardMapper mapper = new BoardMapper(board);
        controller = new Controller(mapper, engine);
    }

    @Test
    void firstClickOnEmptyCell_isIgnored() {
        ControllerResult result = controller.click(150, 50); // (0,1) - ריק

        assertFalse(result.requestedMove());
    }

    @Test
    void firstClickOnAPiece_selectsItWithoutRequestingAMove() {
        ControllerResult result = controller.click(50, 50); // (0,0) - wK

        assertFalse(result.requestedMove()); // בחירה בלבד, אין פקודה ל-GameEngine עדיין
    }

    @Test
    void secondClickOnEmptyCell_requestsAMove() {
        controller.click(50, 50);              // בוחר wK ב-(0,0)
        ControllerResult result = controller.click(50, 150); // (1,0) ריק, בקו-ישר, חוקי למלך

        assertTrue(result.requestedMove());
        assertTrue(result.moveResult().isAccepted());
    }

    @Test
    void secondClickOnFriendlyPiece_sendsRequestMove_notReselect() {
        // שינוי-כלל: קליק-שני *תמיד* שולח request_move, גם על-כלי-
        // ידידותי - בדיוק כמו שהמסמך קובע ("whether the move is
        // legal or illegal"). RuleEngine (לא Controller) מחליט.
        board = new BoardParser().parse("wK wR .\n. . .\n. . .");
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, new RuleEngine(board, new PieceRules()), new RealTimeArbiter(board));
        controller = new Controller(new BoardMapper(board), engine);

        controller.click(50, 50);              // בוחר wK
        ControllerResult result = controller.click(150, 50);  // wR - ידידותי

        assertTrue(result.requestedMove()); // עכשיו *כן* נשלח מהלך
        assertFalse(result.moveResult().isAccepted()); // ונדחה - מלך לא-יכול-לאכול-ידיד
        assertEquals(com.chessgame.rules.MoveReason.FRIENDLY_DESTINATION, result.moveResult().reason());
    }

    @Test
    void knightCanCaptureFriendlyPieceThroughController() {
        // בדיוק הבאג שתוקן: לפני התיקון, קליק-שני-על-ידיד תמיד
        // "נבחר-מחדש" - גם לפרש, שמותר-לו-לטרגט-ידיד. עכשיו זה
        // נגיש דרך Controller, לא רק דרך GameEngine ישירות.
        board = new BoardParser().parse(". . .\n. . wP\n. . .");
        board.addPiece(new com.chessgame.model.Piece("n", com.chessgame.model.Piece.Color.WHITE, com.chessgame.model.Piece.Kind.KNIGHT, new com.chessgame.model.Position(0, 0)));
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, new RuleEngine(board, new PieceRules()), new RealTimeArbiter(board));
        controller = new Controller(new BoardMapper(board), engine);

        controller.click(50, 50);                     // בוחר פרש ב-(0,0)
        ControllerResult result = controller.click(250, 150); // wP ב-(1,2) - ידידותי

        assertTrue(result.requestedMove());
        assertTrue(result.moveResult().isAccepted()); // הפרש *כן* יכול לטרגט את הידיד
    }

    @Test
    void clickOutsideBoard_withNoSelection_isIgnored() {
        ControllerResult result = controller.click(-10, -10);

        assertFalse(result.requestedMove());
    }

    @Test
    void clickOutsideBoard_withActiveSelection_cancelsSelectionWithoutSendingACommand() {
        controller.click(50, 50); // בוחר wK

        ControllerResult result = controller.click(-10, -10); // מבטל

        assertFalse(result.requestedMove()); // בלי שום פקודה ל-GameEngine

        // הוכחה שהבחירה באמת בוטלה: קליק הבא הוא "קליק ראשון" חדש
        // (על תא-ריק) - לא "קליק שני" שהיה שולח מהלך
        ControllerResult next = controller.click(50, 150);
        assertFalse(next.requestedMove());
    }

    @Test
    void secondClickAlwaysClearsSelection_evenWhenMoveIsRejected() {
        controller.click(50, 50); // בוחר wK
        controller.click(250, 250); // יעד רחוק/לא-חוקי למלך - נדחה

        // הבחירה חייבת להתאפס בכל מקרה - קליק הבא הוא "קליק ראשון" חדש
        ControllerResult next = controller.click(50, 150);
        assertFalse(next.requestedMove()); // רק בחירה, לא מהלך - כלומר זה נחשב "ראשון"
    }

    @Test
    void jump_requestsAJumpDirectly() {
        ControllerResult result = controller.jump(150, 50); // bR ב-(0,1)

        assertTrue(result.requestedMove());
        assertTrue(result.moveResult().isAccepted());
    }
}

package com.chessgame.input;

import com.chessgame.engine.GameEngine;
import com.chessgame.engine.GameSnapshot;
import com.chessgame.engine.MoveResult;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.Optional;

/**
 * Controller / קונטרולר
 *
 * תפקיד: מתרגם קליקים לפקודות-משחק. תלוי *רק* ב-BoardMapper וב-
 * GameEngine - בדיוק כמו שהמסמך קובע במפורש. Board *לא* מופיע
 * ברשימת-התלויות - כדי לדעת "האם יש כלי בתא" (קליק ראשון), הקונטרולר
 * שואל את ה-GameSnapshot דרך gameEngine.snapshot(...), לא Board.
 *
 * שינוי חשוב: הוסרה לגמרי לוגיקת "בחירה-מחדש-כשהיעד-ידידותי"
 * (isFriendlyReselect). המסמך קובע במפורש: "On second click, call
 * GameEngine.request_move... Clear selection after every second
 * in-board click, whether the move is legal or illegal" - קליק שני
 * *תמיד* שולח request_move, בלי-תנאי; RuleEngine (לא Controller)
 * מחליט אם זה חוקי. זה גם פותר בעיה אמיתית: פרש שמותר-לו לטרגט
 * ידיד (KnightRule) היה בעבר בלתי-נגיש דרך קליקים בכלל, כי
 * isFriendlyReselect "יירט" את הקליק לפני ש-request_move נשלח.
 */
public final class Controller {
    private final BoardMapper boardMapper;
    private final GameEngine gameEngine;
    private Position selected;

    public Controller(BoardMapper boardMapper, GameEngine gameEngine) {
        this.boardMapper = boardMapper;
        this.gameEngine = gameEngine;
    }

    public Position selectedCell() { return selected; }

    /**
     * מעביר את גודל-המשבצת העדכני ל-BoardMapper הפנימי, בלי לחשוף
     * אותו עצמו החוצה. יש לקרוא לזה מ-GameWindow, לפני כל click(),
     * עם ה-cellSize שחושב הרגע מגודל-הפאנל הנוכחי.
     */
    public void setCellSizePx(int cellSizePx) {
        boardMapper.setCellSizePx(cellSizePx);
    }

    public ControllerResult click(int x, int y) {
        Position cell = boardMapper.pixelToCell(x, y);

        if (cell == null) {
            selected = null;
            return ControllerResult.noMove();
        }

        if (selected == null) {
            return handleFirstClick(cell);
        }

        return handleSecondClick(cell);
    }

    public ControllerResult jump(int x, int y) {
        Position cell = boardMapper.pixelToCell(x, y);
        if (cell == null) {
            return ControllerResult.noMove();
        }

        if (cell.equals(selected)) {
            selected = null;
        }

        return ControllerResult.moveRequested(gameEngine.requestJump(cell));
    }

    private ControllerResult handleFirstClick(Position cell) {
        if (pieceColorAt(cell).isEmpty()) {
            return ControllerResult.noMove();
        }
        selected = cell;
        return ControllerResult.noMove();
    }

    private ControllerResult handleSecondClick(Position cell) {
        MoveResult result = gameEngine.requestMove(selected, cell);
        selected = null;
        return ControllerResult.moveRequested(result);
    }

    /** שואל את ה-GameSnapshot (לא את Board) האם יש כלי בתא, ומה צבעו. */
    private Optional<Piece.Color> pieceColorAt(Position cell) {
        GameSnapshot snapshot = gameEngine.snapshot(selected);
        for (GameSnapshot.PieceView piece : snapshot.pieces()) {
            if (piece.position().equals(cell)) {
                return Optional.of(piece.color());
            }
        }
        return Optional.empty();
    }
}

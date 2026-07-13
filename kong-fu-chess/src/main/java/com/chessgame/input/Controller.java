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
 * GameEngine - בדיוק כמו שהמסמך קובע במפורש ("Controller depends on
 * BoardMapper and GameEngine"). Board *לא* מופיע ברשימת-התלויות שם -
 * ובכוונה: כדי לדעת "האם יש כלי בתא הזה" (קליק ראשון) או "מה
 * הצבע שלו" (בחירה-מחדש), הקונטרולר שואל את ה-GameSnapshot (דרך
 * gameEngine.snapshot(...)) - לא את ה-Board ישירות. זה מונע ממנו
 * לגמרי אפשרות "לגלוש" ולקרוא ל-Board.movePiece בטעות, כי אין לו
 * בכלל רפרנס ל-Board.
 */
public final class Controller {
    private final BoardMapper boardMapper;
    private final GameEngine gameEngine;
    private Position selected;

    public Controller(BoardMapper boardMapper, GameEngine gameEngine) {
        this.boardMapper = boardMapper;
        this.gameEngine = gameEngine;
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
        if (isFriendlyReselect(cell)) {
            selected = cell;
            return ControllerResult.noMove();
        }

        MoveResult result = gameEngine.requestMove(selected, cell);
        selected = null;
        return ControllerResult.moveRequested(result);
    }

    private boolean isFriendlyReselect(Position cell) {
        Optional<Piece.Color> clickedColor = pieceColorAt(cell);
        if (clickedColor.isEmpty()) return false;

        Optional<Piece.Color> selectedColor = pieceColorAt(selected);
        return selectedColor.isPresent() && selectedColor.get() == clickedColor.get();
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

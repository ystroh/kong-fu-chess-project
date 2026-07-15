package com.chessgame.input;

import com.chessgame.model.Board;
import com.chessgame.model.Position;

/**
 * BoardMapper / ממיר-פיקסלים לקונטרולר
 *
 * ממיר קליק גולמי (פיקסלים, יחסית ללוח בלבד - לא ליחסי-פאנל!)
 * לתא לוגי (Position).
 *
 * חשוב: המספרים ש-pixelToCell מקבלת כאן הם *כבר* אחרי שה-UI
 * (GameWindow) הוריד מהם את ה-offset שממרכז את הלוח בפאנל.
 * BoardMapper לא יודע כלום על גודל-חלון או מרכוז - הוא רק יודע
 * "כמה פיקסלים זו משבצת אחת", ומחלק לפי זה.
 *
 * ברירת המחדל (100px) נשמרת כדי שטסטים קיימים שיוצרים
 * BoardMapper בלי לקרוא ל-setCellSizePx בכלל ימשיכו לעבוד בדיוק
 * כמו היום. רק ה-UI האמיתי (עם חלון אמיתי בגודל משתנה) קורא
 * בפועל ל-setCellSizePx, בכל קליק מחדש.
 */
public final class BoardMapper {
    private static final int DEFAULT_CELL_SIZE_PX = 100;

    private final Board board;
    private int cellSizePx = DEFAULT_CELL_SIZE_PX;

    public BoardMapper(Board board) {
        this.board = board;
    }

    /**
     * מעדכן את גודל המשבצת הנוכחי (בפיקסלים), לפי חישוב-הגודל
     * העדכני של הפאנל. יש לקרוא לזה מחדש בכל קליק (ראה GameWindow) -
     * הערך הקודם *לא* נשמר/מוזכר לאחר מכן, רק משמש לחישוב הזה.
     */
    public void setCellSizePx(int cellSizePx) {
        if (cellSizePx <= 0) {
            throw new IllegalArgumentException("cellSizePx must be positive: " + cellSizePx);
        }
        this.cellSizePx = cellSizePx;
    }

    public int cellSizePx() {
        return cellSizePx;
    }

    /** ממיר פיקסלים (יחסית ללוח) לתא-לוח, או null אם מחוץ לגבולות (כולל שליליים). */
    public Position pixelToCell(int x, int y) {
        if (x < 0 || y < 0) return null;
        Position candidate = new Position(y / cellSizePx, x / cellSizePx);
        return board.isInBounds(candidate) ? candidate : null;
    }
}

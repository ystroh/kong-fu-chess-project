package com.chessgame.ui;

import com.chessgame.model.Position;
import java.awt.Point;

/**
 * UiMapper / ממיר-פיקסלים לרנדור
 *
 * ההפך מ-BoardMapper: לא ממיר קליק לתא, אלא ממיר תא-לוגי לפיקסל
 * (על מנת לדעת איפה בדיוק לצייר תמונת-כלי על תמונת-הלוח).
 *
 * שימי לב: cellToPixel *לא* מוסיפה offsetX/offsetY בעצמה - היא
 * מניחה שהיא מציירת "בתוך" תמונת-הלוח כבר, שתוצב אח"כ במיקום
 * ה-offset הנכון על גבי הפאנל (ראה RenderUI.renderNewFrame).
 * זו בדיוק אותה חלוקת-אחריות כמו ב-BoardMapper: המיפוי בין
 * "משבצת" ל"פיקסל" הוא אחריות המחלקה הזאת; המרכוז בפאנל
 * (offsetX/offsetY) הוא אחריות שכבה אחרת, מעליה.
 *
 * ברירת המחדל (100px) נשמרת מטעמי תאימות-לאחור, בדיוק כמו ב-
 * BoardMapper - כל קוד קיים שיוצר UiMapper() בלי setCellSize
 * ימשיך לעבוד באותו גודל-תא קבוע כמו היום.
 */
public final class UiMapper {
    private static final int DEFAULT_CELL_SIZE = 100;

    private int cellSize = DEFAULT_CELL_SIZE;

    public UiMapper() {
    }

    /** מעדכנת את גודל המשבצת הנוכחי (בפיקסלים) - לקרוא בכל רינדור מחדש. */
    public void setCellSize(int cellSize) {
        if (cellSize <= 0) {
            throw new IllegalArgumentException("cellSize must be positive: " + cellSize);
        }
        this.cellSize = cellSize;
    }

    public int getCellSize() {
        return cellSize;
    }

    /**
     * המרה ממיקום לוגי (Position) לפיקסלים על המסך (עבור הציור).
     * שימי לב להצלבה החשובה:
     * col (עמודה אופקית) -> ציר X
     * row (שורה אנכית)   -> ציר Y
     */
    public Point cellToPixel(Position pos) {
        int x = pos.col() * cellSize;
        int y = pos.row() * cellSize;
        return new Point(x, y);
    }

    /**
     * המרה מקליק בעכבר (פיקסלים) בחזרה למיקום לוגי על הלוח.
     * (משמשת רק אם צריך גם מה-ui לדעת "על איזה תא לחצו" - במקרה
     * הרגיל זה תפקידו של BoardMapper/Controller, לא של UiMapper.)
     */
    public Position pixelToCell(int x, int y) {
        if (x < 0 || y < 0) return null;
        return new Position(y / cellSize, x / cellSize);
    }
}

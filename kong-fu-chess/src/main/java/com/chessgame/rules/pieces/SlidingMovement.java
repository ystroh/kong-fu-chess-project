package com.chessgame.rules.pieces;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.Set;

/**
 * SlidingMovement / תנועה-גולשת
 *
 * תפקיד: מוסיף את כל המשבצות בכיוון נתון, עד קצה-הלוח - *בלי*
 * לבדוק תפוסה בכלל. "you can make certain illegal chess moves (such
 * as move through pieces)" - אין יותר חסימת-מסלול ברמת-החוקיות;
 * מה-שקורה-בפועל-בזמן-אמת (מישהו-עדיין-שם-כשמגיעים) מטופל על-ידי
 * CollisionManager/ArrivalResolver, לא כאן.
 */
/**
 * SlidingMovement / תנועה-גולשת
 *
 * תפקיד: מוסיף משבצות בכיוון נתון, עד קצה-הלוח - בהתאם לכלל
 * המדויק: כלי-ידיד באמצע-הדרך *חוסם לגמרי* (לא כלול, ולא ממשיכים
 * מעבר-לו - זו "מהלך לא-חוקי", RuleEngine ידחה אותו). כלי-אויב
 * באמצע-הדרך *לא חוסם בכלל* - כלול, וממשיכים-מעבר-לו גם-כן (זו
 * "התנגשות", לא "חסימה" - תיפתר בזמן-אמת על-ידי CollisionManager,
 * לא כאן).
 */
final class SlidingMovement {
    private SlidingMovement() {
        // מחלקת עזר סטטית בלבד.
    }

    static void addSlidingDirection(Board board, Piece piece, int dRow, int dCol, Set<Position> destinations) {
        int row = piece.cell().row() + dRow;
        int col = piece.cell().col() + dCol;

        while (board.isInBounds(new Position(row, col))) {
            Position current = new Position(row, col);
            Piece occupant = board.pieceAt(current);

            if (occupant != null && occupant.isSameColorAs(piece)) {
                break; // ידיד - חוסם לגמרי, לא כלול
            }

            destinations.add(current); // ריק או אויב - כלול, וממשיכים מעבר-לו
            row += dRow;
            col += dCol;
        }
    }
}

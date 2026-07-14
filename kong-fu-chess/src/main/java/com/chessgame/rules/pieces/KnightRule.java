package com.chessgame.rules.pieces;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.HashSet;
import java.util.Set;

/**
 * KnightRule / חוק פרש
 *
 * תפקיד: קפיצות L. *לא* בודק תפוסה בכלל, כולל תא-ידידותי - "if the
 * knight lands on an occupied spot, that piece must be killed. This
 * is the only way you can kill your own pieces". הבדיקה-היחידה היא
 * גבולות-הלוח; מה-שיקרה-בפועל-בהגעה (לכידה, גם-של-כלי-ידידותי)
 * כבר מטופל נכון ב-ArrivalResolver, בלי צורך בשינוי שם.
 */
public final class KnightRule implements PieceRule {
    private static final int[][] OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    @Override
    public Set<Position> legalDestinations(Board board, Piece piece) {
        Set<Position> destinations = new HashSet<>();
        for (int[] offset : OFFSETS) {
            Position target = new Position(piece.cell().row() + offset[0], piece.cell().col() + offset[1]);
            if (board.isInBounds(target)) {
                destinations.add(target);
            }
        }
        return destinations;
    }
}

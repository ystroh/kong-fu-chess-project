package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.realtime.motion.Motion;

/**
 * ArrivalResolver / מעבד-הגעות (טהור - מסלול משותף בלבד)
 *
 * תפקיד: בדיוק "חוק ההגעה" מהמסמך, 4 שלבים - הסרה ממקור, לכידת
 * אויב ביעד אם קיים, הצבה ביעד, דיווח לכידת-מלך. אין כאן שום ידיעה
 * על קפיצות - המחלקה הזו ניתנת לבדיקה/סקירה בדיוק כאילו פיצ'ר
 * הקפיצה לא קיים בכלל, בהתאם לכלל המסלול הנוסף: "אם תלמידים לא
 * יכולים להוסיף את הפיצ'ר בלי לכתוב מחדש את המסלול המשותף, העיצוב
 * נדחה". התוספת של קפיצה חיה במחלקה אחרת (JumpAwareArrivalResolver),
 * שעוטפת את זו בלי לגעת בה.
 */
public final class ArrivalResolver {
    private final Board board;

    public ArrivalResolver(Board board) {
        this.board = board;
    }

    /** מעבד רשימת תנועות-שהגיעו. מחזיר true אם אחת ההגעות לכדה מלך. */
    public boolean resolveArrivals(Iterable<Motion> arrivedMotions) {
        boolean kingCaptured = false;
        for (Motion motion : arrivedMotions) {
            kingCaptured |= resolveNormalArrival(motion);
        }
        return kingCaptured;
    }

    private boolean resolveNormalArrival(Motion motion) {
        Piece capturedPiece = board.pieceAt(motion.destination());
        board.movePiece(motion.source(), motion.destination());
        motion.piece().setState(Piece.State.IDLE);

        if (capturedPiece != null) {
            capturedPiece.setState(Piece.State.CAPTURED);
            return capturedPiece.kind() == Piece.Kind.KING;
        }
        return false;
    }
}

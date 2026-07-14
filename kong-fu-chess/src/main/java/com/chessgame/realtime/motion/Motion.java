package com.chessgame.realtime.motion;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;

/**
 * Motion / תנועה
 *
 * תפקיד: דאטה בלבד - בלתי-ניתנת-לשינוי לחלוטין (immutable), בדיוק
 * כמו כל שאר מחלקות-הדאטה בפרויקט (Position, MoveResult וכו').
 * "קיצור" של תנועה (למשל בעצירה-בהתנגשות-ידידים) *לא* משנה מופע
 * קיים - הוא בונה מופע-Motion *חדש* ומחליף אותו ב-MotionManager.
 *
 * startTime נוסף כאן (לא היה קודם) - נחוץ כדי לקבוע "מי-התחיל-לזוז-
 * קודם" בהתנגשות-אויבים, לפי הכלל המדויק מהמסמך.
 */
public final class Motion {
    private final Position source;
    private final Position destination;
    private final Piece piece;
    private final long startTime;
    private final long arrivalTime;

    public Motion(Position source, Position destination, Piece piece, long startTime, long arrivalTime) {
        this.source = source;
        this.destination = destination;
        this.piece = piece;
        this.startTime = startTime;
        this.arrivalTime = arrivalTime;
    }

    public Position source() { return source; }
    public Position destination() { return destination; }
    public Piece piece() { return piece; }
    public long startTime() { return startTime; }
    public long arrivalTime() { return arrivalTime; }

    /** האם התנועה הזו כבר הגיעה ליעד שלה, נכון לזמן-המשחק הנתון. */
    boolean hasArrived(long gameClock) { return gameClock >= arrivalTime; }
}

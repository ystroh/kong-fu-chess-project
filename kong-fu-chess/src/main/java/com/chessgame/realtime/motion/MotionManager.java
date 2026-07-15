package com.chessgame.realtime.motion;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * MotionManager / מנהל-תנועות
 *
 * תפקיד: "בעלים" בלעדי של רשימת התנועות-הפעילות (List<Motion>).
 * אחרי הוספת מנגנון-ההתנגשויות, המחלקה הזו *לא* יודעת יותר שום
 * דבר על גיאומטריה/חסימות/התנגשויות - היא רק "מחזיקה רשימה"
 * ועונה שאלות-בסיסיות עליה (מי-בתנועה, מה-הגיע). כל ה"נימוק" על
 * התנגשויות עבר ל-CollisionManager (חבילת collision) - בדיוק
 * כמו ש-Board מחזיק תפוסה ו-RuleEngine מנמק עליה.
 */
public final class MotionManager {
    private final List<Motion> activeMotions = new ArrayList<>();

    /** האם יש תנועה פעילה שיוצאת מהתא הנתון. */
    public boolean isPieceMoving(Position position) {
        for (Motion m : activeMotions) {
            if (m.source().equals(position)) return true;
        }
        return false;
    }

    /**
     * רושם תנועה חדשה כ"פעילה", ומעדכן את מצב הכלי ל-MOVING.
     * מחזיר את ה-Motion שנוצר, כדי שהקורא (RealTimeArbiter) יוכל
     * להעביר אותו הלאה ל-CollisionManager לבדיקת-התנגשויות.
     */
    public Motion startMove(Position source, Position destination, Piece piece, long startTime, long arrivalTime) {
        piece.setState(Piece.State.MOVING);
        Motion motion = new Motion(source, destination, piece, startTime, arrivalTime);
        activeMotions.add(motion);
        return motion;
    }

    /**
     * מחליף תנועה-קיימת בתנועה-חדשה (משמש ב"עצירת-ידידים" - הכלי
     * ממשיך להתקיים, רק עם יעד/זמן-הגעה מקוצרים). אם oldMotion כבר
     * לא ברשימה (למשל טופל-כבר-קודם) - לא עושה כלום, בבטחה.
     */
    public void replace(Motion oldMotion, Motion newMotion) {
        if (activeMotions.remove(oldMotion)) {
            activeMotions.add(newMotion);
        }
    }

    /**
     * מסיר תנועה מהרשימה לגמרי, בלי להחליף (משמש כשכלי נהרג
     * בהתנגשות-אויבים - הוא לא "מגיע" לשום מקום חדש, פשוט נעלם).
     */
    public void remove(Motion motion) {
        activeMotions.remove(motion);
    }

    /**
     * האם המופע-הספציפי-הזה של Motion עדיין "פעיל וקיים" ברשימה.
     * זו בדיוק בדיקת-הרעננות שמונעת טיפול-כפול/מיושן בהתנגשות: אם
     * תנועה הוסרה (נהרגה) או הוחלפה (נעצרה) *לפני* שהגיע-תורה של
     * התנגשות-אחרת שהייתה-אמורה-לערב-אותה, הבדיקה הזו תחזיר false,
     * וההתנגשות-המאוחרת תתעלם בשקט.
     */
    public boolean isStillActive(Motion motion) {
        return activeMotions.contains(motion);
    }

    /** עותק-לקריאה-בלבד של כל התנועות-הפעילות כרגע (לצורך בדיקת-התנגשויות). */
    public List<Motion> activeMotionsSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(activeMotions));
    }

    /** אוסף ומוציא מהרשימה את כל התנועות שהגיעו ליעדן עד לזמן הנתון. */
    public List<Motion> collectArrived(long gameClock) {
        List<Motion> arrived = new ArrayList<>();
        Iterator<Motion> it = activeMotions.iterator();
        while (it.hasNext()) {
            Motion m = it.next();
            if (m.hasArrived(gameClock)) {
                arrived.add(m);
                it.remove();
            }
        }
        return arrived;
    }
    /** מחזירה את ה-Motion הפעיל שיוצא מהתא הנתון, או null אם אין. */
    public Motion motionOf(Position position) {
        for (Motion m : activeMotions) {
            if (m.source().equals(position)) return m;
        }
        return null;
    }
}

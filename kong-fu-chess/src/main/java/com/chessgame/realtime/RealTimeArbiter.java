package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.airborne.AirborneManager;
import com.chessgame.realtime.airborne.JumpAwareArrivalResolver;
import com.chessgame.realtime.collision.CollisionManager;
import com.chessgame.realtime.cooldown.CooldownManager;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

import java.util.List;

/**
 * RealTimeArbiter / בורר-זמן-אמת
 *
 * תפקיד: "שער הכניסה" הציבורי היחיד ל-GameEngine. מתאם בין כל
 * המנגנונים הפנימיים - בלי להכיל שום לוגיקה עצמאית משל עצמו.
 *
 * סדר-הפעולות ב-advanceTime חשוב: (1) CollisionManager קודם - כדי
 * שמהלכים-שקוצרו/הוסרו-בהתנגשות לא "יתפסו" בטעות ב-collectArrived
 * עם היעד-הישן. (2) collectArrived+ArrivalResolver - הגעה-רגילה.
 * (3) קירור - *אחרי* ArrivalResolver, כי רק כלים-שהגיעו-בהצלחה
 * (state==IDLE, לא CAPTURED) אמורים להיכנס לקירור.
 */
public final class RealTimeArbiter {
    private static final int CELL_DURATION_MS = 1000;
    private static final int JUMP_DURATION_MS = 1000;
    private static final int COOLDOWN_DURATION_MS = 1000;

    private final Board board;
    private final MotionManager motionManager = new MotionManager();
    private final AirborneManager airborneManager = new AirborneManager();
    private final CollisionManager collisionManager;
    private final CooldownManager cooldownManager = new CooldownManager(COOLDOWN_DURATION_MS);
    private final ArrivalResolver commonRouteResolver;
    private final JumpAwareArrivalResolver arrivalResolver;
    private long gameClock = 0;

    public RealTimeArbiter(Board board) {
        this.board = board;
        this.collisionManager = new CollisionManager(board, motionManager, CELL_DURATION_MS);
        this.commonRouteResolver = new ArrivalResolver(board);
        this.arrivalResolver = new JumpAwareArrivalResolver(board, commonRouteResolver, airborneManager);
    }

    /**
     * האם מותר להתחיל תנועה ממקור ליעד עכשיו - הכלי לא כבר בתנועה,
     * לא מרחף, ולא בקירור. *אין* בדיקת חסימת-מסלול/יעד-שמור כאן -
     * זו כבר אחריות RuleEngine (חוסם-ידיד) ו-CollisionManager (מטפל
     * באויב-שבדרך, בזמן-אמת).
     */
    public boolean canStartMotion(Position source, Position destination) {
        if (motionManager.isPieceMoving(source)) return false;
        if (airborneManager.isPieceAirborne(source)) return false;
        if (cooldownManager.isPieceCoolingDown(source)) return false;
        return true;
    }

    /** האם מותר להתחיל קפיצה מהתא הנתון עכשיו. */
    public boolean canStartJump(Position position) {
        if (motionManager.isPieceMoving(position)) return false;
        if (airborneManager.isPieceAirborne(position)) return false;
        if (cooldownManager.isPieceCoolingDown(position)) return false;
        return true;
    }

    public void startMotion(Position source, Position destination) {
        Piece piece = board.pieceAt(source);
        int distance = Math.max(
                Math.abs(destination.row() - source.row()),
                Math.abs(destination.col() - source.col())
        );
        long arrivalTime = gameClock + (long) distance * CELL_DURATION_MS;

        List<Motion> othersBefore = motionManager.activeMotionsSnapshot();
        Motion motion = motionManager.startMove(source, destination, piece, gameClock, arrivalTime);
        collisionManager.registerIfColliding(motion, othersBefore);
    }

    public void startJump(Position position) {
        Piece piece = board.pieceAt(position);
        long landTime = gameClock + JUMP_DURATION_MS;
        airborneManager.startJump(position, piece, landTime);
    }

    /** מקדם זמן מדומה. מחזיר true אם הייתה לכידת-מלך, מכל מקור שהוא. */
    public boolean advanceTime(int milliseconds) {
        gameClock += milliseconds;

        boolean kingCapturedByCollision = collisionManager.resolveDue(gameClock);

        List<Motion> arrived = motionManager.collectArrived(gameClock);
        boolean kingCapturedByArrival = arrivalResolver.resolveArrivals(arrived);

        for (Motion motion : arrived) {
            if (motion.piece().state() == Piece.State.IDLE) {
                cooldownManager.startCooldown(motion.piece(), gameClock);
            }
        }

        airborneManager.landExpiredJumps(gameClock);
        cooldownManager.clearExpiredCooldowns(gameClock);

        return kingCapturedByCollision || kingCapturedByArrival;
    }
}

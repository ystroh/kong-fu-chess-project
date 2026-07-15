package com.chessgame.ui;

import com.chessgame.engine.GameSnapshot;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * RenderUI / רנדרר-הלוח
 *
 * תפקיד: מקבלת GameSnapshot, מחזירה BufferedImage מוכן של הלוח.
 * לא יודעת כלום על Swing/JPanel/geometry-של-פאנל - רק "כמה פיקסלים
 * זו משבצת" (דרך UiMapper), ו"מה המצב עכשיו" (דרך snapshot). גודל
 * התמונה שהיא מחזירה נגזר מ-mapper.getCellSize() * snapshot.width()/
 * height() - לא מתקבל כפרמטר נפרד, כדי לא "לדלוף" geometry מבחוץ.
 */
public final class RenderUI {

    private static final String BOARD_IMAGE_PATH = "/board.png";
    private static final String PIECES_BASE_PATH = "/pieces/";
    private static final int FRAME_COUNT = 5;
    private static final Color SELECTED_CELL_COLOR = new Color(80, 200, 120, 110);

    /**
     * מטמון תמונות-שהוטענו-ונמתחו-כבר, לפי "נתיב@רוחבXגובה". בלי
     * זה, כל טיק בלולאה (כל ~33ms) היה טוען-מחדש מהדיסק/classpath
     * את board.png וגם sprite לכל כלי - עבודת-קלט/פלט כבדה שגרמה
     * בפועל ללולאה לרוץ הרבה יותר לאט ממה שתוכנן, ולתנועות להיראות
     * "איטיות בטירוף" (זו לא הייתה בעיה במהירות-המהלך עצמה, אלא
     * ברינדור שלקח הרבה יותר זמן מטיק אחד). המפתח כולל גם את הגודל
     * כי cellSize משתנה עם גודל-החלון - אז המטמון "מתאפס מעצמו"
     * אוטומטית לכל גודל חדש, בלי שצריך לנקות אותו ידנית.
     */
    private static final Map<String, BufferedImage> IMAGE_CACHE = new HashMap<>();

    private final UiMapper mapper;

    public RenderUI(UiMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * מקבלת את מצב המשחק (Snapshot) ומחזירה תמונה (BufferedImage)
     * מעודכנת של הלוח, בגודל הנוכחי (מ-mapper.getCellSize()).
     */
    public BufferedImage renderNewFrame(GameSnapshot snapshot) {
        int cellSize = mapper.getCellSize();
        int imageWidth = cellSize * snapshot.width();
        int imageHeight = cellSize * snapshot.height();

        // הרקע נטען מהמטמון, אבל *מועתק* כאן - כי אנחנו הולכים
        // לצייר עליו (כלים/הדגשה/טקסט) בכל פריים, ואסור "לגרד" את
        // הציור הזה לתוך הגרסה המשותפת-במטמון (אחרת הכלים מהפריים
        // הקודם היו נשארים "צרובים" ברקע לתמיד).
        BufferedImage cachedBackground = cachedImage(BOARD_IMAGE_PATH, imageWidth, imageHeight, false);
        Img finalBoardImage = Img.wrap(copyOf(cachedBackground));

        if (snapshot.selectedCell() != null) {
            drawSelectedCellHighlight(finalBoardImage, snapshot.selectedCell(), cellSize);
        }

        for (GameSnapshot.PieceView piece : snapshot.pieces()) {
            drawPiece(finalBoardImage, piece, cellSize);
        }

        if (snapshot.isGameOver()) {
            drawGameOverText(finalBoardImage, snapshot.winner());
        }

        return finalBoardImage.get();
    }

    /**
     * מחזירה תמונה בגודל המבוקש, מהמטמון אם כבר נטענה, או טוענת
     * (ומכניסה למטמון) אם זו הפעם הראשונה שמבקשים את הצירוף הזה
     * של נתיב+גודל.
     */
    private BufferedImage cachedImage(String path, int width, int height, boolean keepAspect) {
        String key = path + "@" + width + "x" + height;
        return IMAGE_CACHE.computeIfAbsent(key,
                k -> new Img().read(path, new Dimension(width, height), keepAspect, null).get());
    }

    /** מעתיקה תמונה, כדי שציור-עליה לא ישנה את המקור (המשותף, מהמטמון). */
    private BufferedImage copyOf(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    /** מצייר ריבוע-הדגשה חצי-שקוף על המשבצת הנבחרת, אם יש כזו. */
    private void drawSelectedCellHighlight(Img boardImage, Position selectedCell, int cellSize) {
        Point pixel = mapper.cellToPixel(selectedCell);
        Graphics2D g = boardImage.get().createGraphics();
        g.setColor(SELECTED_CELL_COLOR);
        g.fillRect(pixel.x, pixel.y, cellSize, cellSize);
        g.dispose();
    }

    /** טוען את ה-sprite הנכון לכלי (לפי מצבו הנוכחי, מהמטמון) ומדביק אותו במקומו. */
    private void drawPiece(Img boardImage, GameSnapshot.PieceView piece, int cellSize) {
        String path = spritePath(piece);
        if (path == null) {
            return; // אין sprite למצב הזה (למשל CAPTURED) - לא מציירים כלום
        }

        BufferedImage cachedPiece = cachedImage(path, cellSize, cellSize, true);
        Img pieceImg = Img.wrap(cachedPiece);
        Point pixel = mapper.cellToPixel(piece.position());
        pieceImg.drawOn(boardImage, pixel.x, pixel.y);
    }

    private void drawGameOverText(Img boardImage, Piece.Color winner) {
        String winnerText = "GAME OVER";
        if (winner != null) {
            winnerText += " - " + (winner == Piece.Color.WHITE ? "White Wins!" : "Black Wins!");
        }
        boardImage.putText(winnerText, 20, 40, 2.0f, Color.RED, 1);
    }

    /**
     * מרכיבה את נתיב-הקובץ המלא לפריים הנוכחי של הכלי, לפי הדפוס:
     * pieces/<CODE>/states/<state>/sprites/<frame>.png
     * מחזירה null אם אין אנימציה מתאימה למצב (למשל CAPTURED).
     */
    private String spritePath(GameSnapshot.PieceView piece) {
        String stateFolder = stateFolder(piece.state());
        if (stateFolder == null) {
            return null;
        }
        String code = pieceFolderCode(piece.color(), piece.kind());
        int frame = currentFrameIndex(piece.state());
        return PIECES_BASE_PATH + code + "/states/" + stateFolder + "/sprites/" + frame + ".png";
    }

    /** ממפה Piece.State לתיקיית-האנימציה המתאימה בחבילת ה-sprites. */
    private String stateFolder(Piece.State state) {
        switch (state) {
            case IDLE: return "idle";
            case MOVING: return "move";
            case AIRBORNE: return "jump";
            case COOLDOWN: return cooldownFolder();
            case CAPTURED: return null;
            default: return null;
        }
    }

    /**
     * נקודת-הרחבה מיועדת: כרגע Piece.State מכיל ערך COOLDOWN יחיד,
     * אז תמיד מוחזר long_rest. כש-Piece יבחין בין "cooldown אחרי
     * קפיצה" ל"cooldown אחרי מהלך-רגיל" (למשל ע"י פיצול ה-enum, או
     * דגל נוסף), רק המקום הזה צריך להשתנות - להחזיר "short_rest"
     * כשרלוונטי, בלי לגעת בשאר הקובץ.
     */
    private String cooldownFolder() {
        return "long_rest";
    }

    /** ממפה צבע+סוג לקוד-התיקייה בנוטציית-שחמט רגילה (למשל: לבן+צריח -> "RW"). */
    private String pieceFolderCode(Piece.Color color, Piece.Kind kind) {
        String kindLetter;
        switch (kind) {
            case KING: kindLetter = "K"; break;
            case QUEEN: kindLetter = "Q"; break;
            case ROOK: kindLetter = "R"; break;
            case BISHOP: kindLetter = "B"; break;
            case KNIGHT: kindLetter = "N"; break;
            case PAWN: kindLetter = "P"; break;
            default: throw new IllegalArgumentException("Unknown piece kind: " + kind);
        }
        String colorLetter = (color == Piece.Color.WHITE) ? "W" : "B";
        return kindLetter + colorLetter;
    }

    /**
     * בוחרת פריים (1..FRAME_COUNT) לפי שעון גלובלי - כי GameSnapshot
     * לא שומר "מתי הכלי נכנס למצב הזה". רק MOVING/AIRBORNE מסתובבים
     * בין פריימים (זו התנועה שרוצים לראות) - IDLE/COOLDOWN מוצגים
     * תמיד עם פריים 1 קבוע, כדי שכלי-שלא-זז לא "ירקוד" בלי סיבה.
     */
    private int currentFrameIndex(Piece.State state) {
        if (state != Piece.State.MOVING && state != Piece.State.AIRBORNE) {
            return 1;
        }
        int fps = framesPerSecondFor(state);
        long elapsedFrames = (System.currentTimeMillis() * fps) / 1000;
        return (int) (elapsedFrames % FRAME_COUNT) + 1;
    }

    /** קצב-הפריימים לאנימציות-בתנועה, כפי שמוגדר ב-config.json שלהן. */
    private int framesPerSecondFor(Piece.State state) {
        return state == Piece.State.AIRBORNE ? 8 : 12;
    }
}

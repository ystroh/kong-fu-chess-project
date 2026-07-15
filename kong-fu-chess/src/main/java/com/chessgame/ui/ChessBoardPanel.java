package com.chessgame.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * ChessBoardPanel / משטח-הציור
 *
 * תפקיד: מציג תמונה מוכנה, במיקום נתון. לא יודע שום דבר על שחמט,
 * snapshot, או מנוע-משחק - רק "יש לי תמונה, ויש לה מיקום, תציירי
 * אותה שם". GameWindow הוא זה שמחליט מה התמונה ואיפה (geometry) -
 * הפאנל רק "מציית" ומצייר.
 */
public class ChessBoardPanel extends JPanel {
    private BufferedImage currentBoardImage;
    private int imageX;
    private int imageY;

    /**
     * מעדכן את התמונה הנוכחית ואת המיקום שבו יש לצייר אותה, ומבקש
     * מ-Swing לרענן את המסך. נקרא מ-GameWindow.render(), עם boardSize
     * ו-offsetX/offsetY שחושבו הרגע מהגודל הנוכחי של הפאנל.
     */
    public void setBoardImage(BufferedImage image, int offsetX, int offsetY) {
        this.currentBoardImage = image;
        this.imageX = offsetX;
        this.imageY = offsetY;

        // קורא ל-paintComponent מאחורי הקלעים ומעדכן את המסך
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // מנקה את הרקע הקודם

        if (currentBoardImage != null) {
            // מצייר את התמונה במיקום ה-offset שחושב מחדש בכל render -
            // לא עוד ב-(0,0) קבוע, אחרת הלוח לא היה נראה ממורכז.
            g.drawImage(currentBoardImage, imageX, imageY, this);
        }
    }
}

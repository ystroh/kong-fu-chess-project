package com.chessgame.ui;
import javax.swing.*;
import java.awt.*;

public class GameApp {
    private JFrame frame;
    private ChessBoardPanel boardPanel;

    public void init() {
        frame = new JFrame("Chess Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        boardPanel = new ChessBoardPanel();
        frame.add(boardPanel); // מוסיפים את הפאנל לחלון

        frame.setSize(800, 800);
        frame.setVisible(true);
    }

    // נקרא לזה בכל פעם שיש שינוי
    public void onGameStateChanged() {
//       Img updatedImage = renderNewFrame(); // פונקציה שלך שמייצרת את תמונת הלוח והכלים
//       boardPanel.setBoardImage(updatedImage); // מעדכן את הפאנל הקיים
    }
}

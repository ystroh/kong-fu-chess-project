package com.chessgame;
/**
 * מייצג כלי שנמצא כרגע באוויר (מבצע קפיצה) וטרם נחת.
 * הכלי נשאר לוגית באותה משבצת עד תום זמן הקפיצה.
 */
class AirborneMove {
    int row, col;
    String piece;
    long landTime;

    /**
     * בנאי ליצירת קפיצה חדשה.
     */
    public AirborneMove(int row, int col, String piece, long landTime) {
        this.row = row;
        this.col = col;
        this.piece = piece;
        this.landTime = landTime;
    }
}
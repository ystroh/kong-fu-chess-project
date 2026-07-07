package com.chessgame;
/**
 * מייצג מהלך פעיל שמתבצע כרגע על הלוח.
 */
class ActiveMove {
    int fromRow, fromCol;
    int toRow, toCol;
    String piece;
    long arrivalTime;

    /**
     * בנאי ליצירת מהלך חדש.
     */
    public ActiveMove(int fromRow, int fromCol, int toRow, int toCol, String piece, long arrivalTime) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.piece = piece;
        this.arrivalTime = arrivalTime;
    }
}
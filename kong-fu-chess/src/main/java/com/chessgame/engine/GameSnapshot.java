package com.chessgame.engine;
import com.chessgame.model.Board;
public final class GameSnapshot {
    private final Board board;
    private final boolean gameOver;
    public GameSnapshot(Board board, boolean gameOver) { this.board = board; this.gameOver = gameOver; }
    public Board board() { return board; }
    public boolean isGameOver() { return gameOver; }
}

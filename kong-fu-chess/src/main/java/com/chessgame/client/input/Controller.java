package com.chessgame.client.input;

import com.chessgame.common.engine.GameSnapshot;
import com.chessgame.common.engine.MoveChannel;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

import java.util.Optional;

public final class Controller {
    private final BoardMapper boardMapper;
    private final MoveChannel channel;
    private final Piece.Color myColor;
    private Position selected;

    public Controller(BoardMapper boardMapper, MoveChannel channel) {
        this(boardMapper, channel, null);
    }

    public Controller(BoardMapper boardMapper, MoveChannel channel, Piece.Color myColor) {
        this.boardMapper = boardMapper;
        this.channel = channel;
        this.myColor = myColor;
    }

    public Position selectedCell() {
        return selected;
    }

    public void setCellSizePx(int cellSizePx) {
        boardMapper.setCellSizePx(cellSizePx);
    }

    public ControllerResult click(int x, int y) {
        Position cell = boardMapper.pixelToCell(x, y);

        if (cell == null) {
            selected = null;
            return ControllerResult.noMove();
        }

        if (selected == null) {
            return handleFirstClick(cell);
        }

        return handleSecondClick(cell);
    }

    public ControllerResult jump(int x, int y) {
        Position cell = boardMapper.pixelToCell(x, y);
        if (cell == null) {
            return ControllerResult.noMove();
        }

        if (cell.equals(selected)) {
            selected = null;
        }

        return ControllerResult.moveRequested(channel.requestJump(cell));
    }

    private ControllerResult handleFirstClick(Position cell) {
        Optional<Piece.Color> colorHere = pieceColorAt(cell);
        if (colorHere.isEmpty()) {
            return ControllerResult.noMove();
        }
        if (myColor != null && colorHere.get() != myColor) {
            return ControllerResult.noMove();
        }
        selected = cell;
        return ControllerResult.noMove();
    }

    private ControllerResult handleSecondClick(Position cell) {
        Position from = selected;
        selected = null;

        if (cell.equals(from)) {
            return ControllerResult.moveRequested(channel.requestJump(cell));
        }

        return ControllerResult.moveRequested(channel.requestMove(from, cell));
    }

    private Optional<Piece.Color> pieceColorAt(Position cell) {
        GameSnapshot snapshot = channel.snapshot(selected);
        if (snapshot == null) {
            return Optional.empty();
        }
        for (GameSnapshot.PieceView piece : snapshot.pieces()) {
            if (piece.position().equals(cell)) {
                return Optional.of(piece.color());
            }
        }
        return Optional.empty();
    }
}
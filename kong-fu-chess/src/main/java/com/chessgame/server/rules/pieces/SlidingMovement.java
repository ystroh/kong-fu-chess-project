package com.chessgame.server.rules.pieces;

import com.chessgame.common.model.Board;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

import java.util.Set;

final class SlidingMovement {
    private SlidingMovement() {
    }

    static void addSlidingDirection(Board board, Piece piece, int dRow, int dCol, Set<Position> destinations) {
        int row = piece.cell().row() + dRow;
        int col = piece.cell().col() + dCol;

        while (board.isInBounds(new Position(row, col))) {
            Position current = new Position(row, col);
            Piece occupant = board.pieceAt(current);

            if (occupant != null && occupant.isSameColorAs(piece)) {
                break;
            }

            destinations.add(current);
            row += dRow;
            col += dCol;
        }
    }
}

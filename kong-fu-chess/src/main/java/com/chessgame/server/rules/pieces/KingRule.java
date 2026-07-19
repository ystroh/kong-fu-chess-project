package com.chessgame.server.rules.pieces;



import com.chessgame.common.model.Board;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

import java.util.HashSet;
import java.util.Set;

public final class KingRule implements PieceRule {
    private static final int[][] OFFSETS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    @Override
    public Set<Position> legalDestinations(Board board, Piece piece) {
        Set<Position> destinations = new HashSet<>();
        for (int[] offset : OFFSETS) {
            Position target = new Position(piece.cell().row() + offset[0], piece.cell().col() + offset[1]);
            if (!board.isInBounds(target)) continue;

            Piece occupant = board.pieceAt(target);
            if (occupant == null || occupant.isEnemyOf(piece)) {
                destinations.add(target);
            }
        }
        return destinations;
    }
}
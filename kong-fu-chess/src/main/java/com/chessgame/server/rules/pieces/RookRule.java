package com.chessgame.server.rules.pieces;
import com.chessgame.common.model.Board;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

import java.util.HashSet;
import java.util.Set;

public final class RookRule implements PieceRule {
    @Override
    public Set<Position> legalDestinations(Board board, Piece piece) {

        Set<Position> destinations = new HashSet<>();

        SlidingMovement.addSlidingDirection(board, piece, -1, 0, destinations);
        SlidingMovement.addSlidingDirection(board, piece, 1, 0, destinations);
        SlidingMovement.addSlidingDirection(board, piece, 0, -1, destinations);
        SlidingMovement.addSlidingDirection(board, piece, 0, 1, destinations);
        return destinations;
    }
}

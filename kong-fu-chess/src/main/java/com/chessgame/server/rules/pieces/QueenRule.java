package com.chessgame.server.rules.pieces;


import com.chessgame.common.model.Board;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

import java.util.HashSet;
import java.util.Set;

public final class QueenRule implements PieceRule {
    private final RookRule asRook = new RookRule();
    private final BishopRule asBishop = new BishopRule();

    @Override
    public Set<Position> legalDestinations(Board board, Piece piece) {
        Set<Position> destinations = new HashSet<>(asRook.legalDestinations(board, piece));
        destinations.addAll(asBishop.legalDestinations(board, piece));
        return destinations;
    }
}

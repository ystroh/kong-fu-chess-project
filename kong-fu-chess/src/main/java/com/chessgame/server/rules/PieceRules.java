package com.chessgame.server.rules;
import com.chessgame.common.model.Board;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;
import com.chessgame.server.rules.pieces.*;
import com.chessgame.server.rules.pieces.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class PieceRules {
    private final Map<Piece.Kind, PieceRule> rules = new EnumMap<>(Piece.Kind.class);

    public PieceRules() {
        rules.put(Piece.Kind.ROOK, new RookRule());
        rules.put(Piece.Kind.BISHOP, new BishopRule());
        rules.put(Piece.Kind.QUEEN, new QueenRule());
        rules.put(Piece.Kind.KNIGHT, new KnightRule());
        rules.put(Piece.Kind.KING, new KingRule());
        rules.put(Piece.Kind.PAWN, new PawnRule());
    }

    public Set<Position> legalDestinations(Board board, Piece piece) {
        return rules.get(piece.kind()).legalDestinations(board, piece);
    }
}

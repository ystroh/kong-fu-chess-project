package com.chessgame.server.rules.pieces;


import com.chessgame.common.model.Board;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

import java.util.Set;


public interface PieceRule {
    Set<Position> legalDestinations(Board board, Piece piece);
}

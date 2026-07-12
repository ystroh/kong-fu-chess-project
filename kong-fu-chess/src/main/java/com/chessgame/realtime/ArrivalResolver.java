package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.realtime.motion.Motion;

public final class ArrivalResolver {
    private final Board board;

    public ArrivalResolver(Board board) {
        this.board = board;
    }

    /** מעבד רשימת תנועות-שהגיעו. מחזיר true אם אחת ההגעות לכדה מלך. */
    public boolean resolveArrivals(Iterable<Motion> arrivedMotions) {
        boolean kingCaptured = false;
        for (Motion motion : arrivedMotions) {
            kingCaptured |= resolveNormalArrival(motion);
        }
        return kingCaptured;
    }

    private boolean resolveNormalArrival(Motion motion) {
        Piece capturedPiece = board.pieceAt(motion.destination());
        board.movePiece(motion.source(), motion.destination());
        motion.piece().setState(Piece.State.IDLE);

        if (capturedPiece != null) {
            capturedPiece.setState(Piece.State.CAPTURED);
            return capturedPiece.kind() == Piece.Kind.KING;
        }
        return false;
    }
}

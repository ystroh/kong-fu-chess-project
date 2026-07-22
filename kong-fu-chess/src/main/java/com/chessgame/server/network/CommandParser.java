package com.chessgame.server.network;

import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;
import com.chessgame.common.protocol.request.JumpMessage;
import com.chessgame.common.protocol.request.MoveMessage;
import com.chessgame.server.Command;

public final class CommandParser {

    public Command parseMove(MoveMessage message, Piece.Color color) {
        Position from = parseSquare(message.notation().substring(0, 2));
        Position to = parseSquare(message.notation().substring(2, 4));
        return new Command.Move(color, from, to);
    }

    public Command parseJump(JumpMessage message, Piece.Color color) {
        Position at = parseSquare(message.notation());
        return new Command.Jump(color, at);
    }

    private Position parseSquare(String square) {
        int col = square.charAt(0) - 'a';
        int rank = Character.getNumericValue(square.charAt(1));
        int row = 8 - rank;
        return new Position(row, col);
    }
}
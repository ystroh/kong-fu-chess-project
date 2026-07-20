package com.chessgame.server.network;

import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;
import com.chessgame.server.Command;

public final class CommandParser {

    public Command parse(String message, Piece.Color color) {
        String[] parts = message.trim().split("\\s+");
        if (parts.length != 2) {
            return null;
        }

        if (parts[0].equals("MOVE") && parts[1].length() == 4) {
            Position from = parseSquare(parts[1].substring(0, 2));
            Position to = parseSquare(parts[1].substring(2, 4));
            return new Command.Move(color, from, to);
        }

        if (parts[0].equals("JUMP") && parts[1].length() == 2) {
            Position at = parseSquare(parts[1]);
            return new Command.Jump(color, at);
        }

        return null;
    }

    private Position parseSquare(String square) {
        int col = square.charAt(0) - 'a';
        int rank = Character.getNumericValue(square.charAt(1));
        int row = 8 - rank;
        return new Position(row, col);
    }
}

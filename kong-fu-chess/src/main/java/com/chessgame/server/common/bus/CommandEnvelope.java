package com.chessgame.server.common.bus;

import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;
import com.chessgame.server.common.Command;

public record CommandEnvelope(String kind, Piece.Color playerColor, Position position, Position to) {

    public static CommandEnvelope of(Command command) {
        if (command instanceof Command.Move move) {
            return new CommandEnvelope("MOVE", move.playerColor(), move.from(), move.to());
        }
        if (command instanceof Command.Jump jump) {
            return new CommandEnvelope("JUMP", jump.playerColor(), jump.at(), null);
        }
        if (command instanceof Command.Resign resign) {
            return new CommandEnvelope("RESIGN", resign.playerColor(), null, null);
        }
        throw new IllegalArgumentException("Unknown command: " + command);
    }

    public Command toCommand() {
        return switch (kind) {
            case "MOVE" -> new Command.Move(playerColor, position, to);
            case "JUMP" -> new Command.Jump(playerColor, position);
            case "RESIGN" -> new Command.Resign(playerColor);
            default -> throw new IllegalArgumentException("Unknown kind: " + kind);
        };
    }
}
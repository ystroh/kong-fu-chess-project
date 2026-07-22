package com.chessgame.server;


import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

public sealed interface Command permits Command.Move, Command.Jump, Command.Resign {
    Piece.Color playerColor();

    record Move(Piece.Color playerColor, Position from, Position to) implements Command {}
    record Jump(Piece.Color playerColor, Position at) implements Command {}
    record Resign(Piece.Color playerColor) implements Command {}
}

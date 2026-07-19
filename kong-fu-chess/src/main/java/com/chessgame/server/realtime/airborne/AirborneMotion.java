package com.chessgame.server.realtime.airborne;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

public final class AirborneMotion {
    final Position cell;
    final Piece piece;
    final long landTime;

    AirborneMotion(Position cell, Piece piece, long landTime) {
        this.cell = cell;
        this.piece = piece;
        this.landTime = landTime;
    }

    boolean wasAirborneAt(long time) { return landTime >= time; }
    boolean hasLanded(long gameClock) { return gameClock >= landTime; }
}

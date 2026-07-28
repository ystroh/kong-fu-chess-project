package com.chessgame.server.realtime.cooldown;

import com.chessgame.common.model.Piece;

final class CooldownEntry {
    final Piece piece;
    final long startTime;
    final long expireTime;

    CooldownEntry(Piece piece, long startTime, long expireTime) {
        this.piece = piece;
        this.startTime = startTime;
        this.expireTime = expireTime;
    }
}

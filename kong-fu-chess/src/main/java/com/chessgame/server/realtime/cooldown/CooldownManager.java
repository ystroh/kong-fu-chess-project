package com.chessgame.server.realtime.cooldown;

import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CooldownManager {
    private static final long LONG_COOLDOWN_MS = 1000;
    private static final long SHORT_COOLDOWN_MS = 400;

    public record CooldownWindow(long startTime, long endTime) {
    }

    private final List<CooldownEntry> entries = new ArrayList<>();

    public boolean isPieceCoolingDown(Position position) {
        for (CooldownEntry entry : entries) {
            if (entry.piece.cell().equals(position)) return true;
        }
        return false;
    }

    public CooldownWindow cooldownOf(Position position) {
        for (CooldownEntry entry : entries) {
            if (entry.piece.cell().equals(position)) {
                return new CooldownWindow(entry.startTime, entry.expireTime);
            }
        }
        return null;
    }

    public void startLongCooldown(Piece piece, long gameClock) {
        piece.setState(Piece.State.COOLDOWN_LONG);
        entries.add(new CooldownEntry(piece, gameClock, gameClock + LONG_COOLDOWN_MS));
    }

    public void startShortCooldown(Piece piece, long gameClock) {
        piece.setState(Piece.State.COOLDOWN_SHORT);
        entries.add(new CooldownEntry(piece, gameClock, gameClock + SHORT_COOLDOWN_MS));
    }

    public void clearExpiredCooldowns(long gameClock) {
        Iterator<CooldownEntry> it = entries.iterator();
        while (it.hasNext()) {
            CooldownEntry entry = it.next();
            if (gameClock >= entry.expireTime) {
                boolean stillCoolingDown = entry.piece.state() == Piece.State.COOLDOWN_LONG
                        || entry.piece.state() == Piece.State.COOLDOWN_SHORT;
                if (stillCoolingDown) {
                    entry.piece.setState(Piece.State.IDLE);
                }
                it.remove();
            }
        }
    }
}

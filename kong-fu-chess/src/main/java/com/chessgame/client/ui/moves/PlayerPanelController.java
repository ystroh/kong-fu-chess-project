package com.chessgame.client.ui.moves;

import com.chessgame.client.ui.GameStateCoordinator;
import com.chessgame.client.ui.SnapshotListener;
import com.chessgame.common.engine.GameSnapshot;
import com.chessgame.common.engine.MoveRecord;
import com.chessgame.common.model.Piece;

import java.util.ArrayList;
import java.util.List;

public final class PlayerPanelController implements SnapshotListener {

    private final Piece.Color color;
    private final MoveHistoryPanel panel;
    private int lastKnownMoveCount = -1;
    private int lastKnownScore = -1;

    public PlayerPanelController(GameStateCoordinator coordinator, Piece.Color color, String playerName) {
        this.color = color;
        this.panel = new MoveHistoryPanel(playerName);

        coordinator.addListener(this);
    }

    public MoveHistoryPanel panel() {
        return panel;
    }

    public void showOpponentStatus(String message) {
        panel.setStatusMessage(message);
    }

    @Override
    public void onSnapshotUpdated(GameSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        List<MoveRecord> mine = new ArrayList<>();
        for (MoveRecord move : snapshot.moveHistory()) {
            if (move.color() == color) {
                mine.add(move);
            }
        }

        if (mine.size() != lastKnownMoveCount) {
            List<String[]> rows = new ArrayList<>();
            for (MoveRecord move : mine) {
                rows.add(new String[]{
                        MoveHistoryFormatter.formatTime(move.timestamp()),
                        MoveHistoryFormatter.formatMove(move)
                });
            }
            panel.setRows(rows);
            lastKnownMoveCount = mine.size();
        }

        int score = snapshot.scores().getOrDefault(color, 0);
        if (score != lastKnownScore) {
            panel.setScore(score);
            lastKnownScore = score;
        }
    }
}

package com.chessgame.server.application;

import com.chessgame.common.model.Piece;
import com.chessgame.server.network.ConnectionSession;
import com.chessgame.server.GameMatch;
import com.chessgame.server.PlayerConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReconnectionManager {

    private static final int GRACE_SECONDS = 20;

    private record PendingDisconnect(ConnectionSession oldSession, Thread countdownThread) {}

    private final Map<String, PendingDisconnect> pending = new ConcurrentHashMap<>();

    public void handleDisconnect(ConnectionSession session) {
        String username = session.username();
        GameMatch match = session.match();
        Piece.Color color = colorOf(session);
        PlayerConnection opponent = match.opponentOf(color);

        Thread countdownThread = new Thread(() -> runCountdown(username, match, color, opponent));
        pending.put(username, new PendingDisconnect(session, countdownThread));
        countdownThread.start();
    }

    public boolean tryReconnect(String username, ConnectionSession newSession) {
        PendingDisconnect disconnect = pending.remove(username);
        if (disconnect == null) {
            return false;
        }

        disconnect.countdownThread().interrupt();

        ConnectionSession oldSession = disconnect.oldSession();
        newSession.setPlayerConnection(oldSession.playerConnection());
        newSession.setMatch(oldSession.match());
        newSession.setState(ConnectionSession.State.IN_GAME);

        Piece.Color color = colorOf(newSession);
        newSession.match().opponentOf(color).send("OPPONENT_RECONNECTED");
        return true;
    }

    private void runCountdown(String username, GameMatch match, Piece.Color color, PlayerConnection opponent) {
        for (int remaining = GRACE_SECONDS; remaining > 0; remaining--) {
            try {
                opponent.send("OPPONENT_DISCONNECTED:" + remaining);
            } catch (Exception e) {
                // גם-היריב-כבר-לא-מחובר - אין-למי-לשלוח, אבל-הספירה-ממשיכה-כרגיל
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (pending.remove(username) != null) {
            match.resign(color);
        }
    }

    private Piece.Color colorOf(ConnectionSession session) {
        return session.playerConnection().role() == PlayerConnection.Role.WHITE
                ? Piece.Color.WHITE : Piece.Color.BLACK;
    }
}
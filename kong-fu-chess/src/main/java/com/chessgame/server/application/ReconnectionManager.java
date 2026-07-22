package com.chessgame.server.application;

import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.response.OpponentDisconnectedMessage;
import com.chessgame.common.protocol.response.OpponentReconnectedMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.server.ConnectionSession;
import com.chessgame.server.GameMatch;
import com.chessgame.server.network.ClientGateway;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReconnectionManager {

    private static final int GRACE_SECONDS = 20;

    private record PendingDisconnect(ConnectionSession oldSession, Thread countdownThread) {}

    private final ClientGateway gateway;
    private final Map<String, PendingDisconnect> pending = new ConcurrentHashMap<>();

    public ReconnectionManager(ClientGateway gateway) {
        this.gateway = gateway;
    }

    public void handleDisconnect(ConnectionSession session) {
        String username = session.username();
        GameMatch match = session.match();
        Piece.Color color = session.color();
        String opponentUsername = match.opponentUsernameOf(color);

        Thread countdownThread = new Thread(() -> runCountdown(username, match, color, opponentUsername));
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
        newSession.setColor(oldSession.color());
        newSession.setMatch(oldSession.match());
        newSession.setState(ConnectionSession.State.IN_GAME);

        String opponentUsername = newSession.match().opponentUsernameOf(newSession.color());
        gateway.sendTo(opponentUsername, ServerMessageType.OPPONENT_RECONNECTED, new OpponentReconnectedMessage());
        return true;
    }

    private void runCountdown(String username, GameMatch match, Piece.Color color, String opponentUsername) {
        for (int remaining = GRACE_SECONDS; remaining > 0; remaining--) {
            gateway.sendTo(opponentUsername, ServerMessageType.OPPONENT_DISCONNECTED,
                    new OpponentDisconnectedMessage(remaining));
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
}

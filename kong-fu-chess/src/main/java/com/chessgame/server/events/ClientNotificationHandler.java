package com.chessgame.server.events;

import com.chessgame.common.protocol.response.*;
import com.chessgame.server.network.ClientGateway;

import java.util.List;

public final class ClientNotificationHandler {

    private final String whiteUsername;
    private final String blackUsername;
    private final List<String> spectatorUsernames;
    private final ClientGateway gateway;

    public ClientNotificationHandler(String whiteUsername, String blackUsername,
                                      List<String> spectatorUsernames, ClientGateway gateway) {
        this.whiteUsername = whiteUsername;
        this.blackUsername = blackUsername;
        this.spectatorUsernames = spectatorUsernames;
        this.gateway = gateway;
    }

    public void onSnapshotUpdated(SnapshotUpdatedEvent event) {
        GameStateMessage msg = new GameStateMessage(event.snapshot());
        broadcast(ServerMessageType.GAME_STATE, msg);
    }

    public void onActionOccurred(ActionOccurredEvent event) {
        ActionOccurredMessage msg = new ActionOccurredMessage(event.actionType(), event.color(),
                event.from(), event.to(), event.capture(), event.gameOver(), event.winner());
        broadcast(ServerMessageType.ACTION_OCCURRED, msg);
    }

    private void broadcast(ServerMessageType type, Object msg) {
        gateway.sendTo(whiteUsername, type, msg);
        gateway.sendTo(blackUsername, type, msg);
        for (String spectator : spectatorUsernames) {
            gateway.sendTo(spectator, type, msg);
        }
    }

    public void onDisconnectStatus(DisconnectStatusEvent event) {
        String opponent = event.disconnectedColor() == com.chessgame.common.model.Piece.Color.WHITE
                ? blackUsername : whiteUsername;
        gateway.sendTo(opponent, ServerMessageType.OPPONENT_DISCONNECTED,
                new OpponentDisconnectedMessage(event.remainingSeconds()));
    }

    public void onReconnect(ReconnectEvent event) {
        String opponent = event.reconnectedColor() == com.chessgame.common.model.Piece.Color.WHITE
                ? blackUsername : whiteUsername;
        gateway.sendTo(opponent, ServerMessageType.OPPONENT_RECONNECTED, new OpponentReconnectedMessage());
    }
}

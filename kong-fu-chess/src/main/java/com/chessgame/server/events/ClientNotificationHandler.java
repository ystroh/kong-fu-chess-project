package com.chessgame.server.events;

import com.chessgame.common.protocol.response.GameStateMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
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
        gateway.sendTo(whiteUsername, ServerMessageType.GAME_STATE, msg);
        gateway.sendTo(blackUsername, ServerMessageType.GAME_STATE, msg);
        for (String spectator : spectatorUsernames) {
            gateway.sendTo(spectator, ServerMessageType.GAME_STATE, msg);
        }
    }
}

package com.chessgame.server.allocator;

import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.response.ParticipantRole;
import com.chessgame.common.protocol.response.RoleMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.server.bus.GameEnded;
import com.chessgame.server.bus.MatchAssignment;
import com.chessgame.server.bus.MatchFound;
import com.chessgame.server.bus.NatsEventBus;
import com.chessgame.server.bus.NatsSubjects;
import com.chessgame.server.bus.SessionAssignment;
import com.chessgame.server.network.MessageSerializer;

import java.util.UUID;

public final class GameAllocatorController {

    private final ShardCapacityRegistry shardCapacityRegistry;
    private final NatsEventBus bus;

    public GameAllocatorController(ShardCapacityRegistry shardCapacityRegistry, NatsEventBus bus) {
        this.shardCapacityRegistry = shardCapacityRegistry;
        this.bus = bus;
    }

    public void start() {
        bus.subscribe(NatsSubjects.matchFound(), MatchFound.class, this::onMatchFound);
    }

    private void onMatchFound(MatchFound matchFound) {
        String shardId = shardCapacityRegistry.pickShardForNewGame();
        if (shardId == null) {
            return;
        }

        String gameId = UUID.randomUUID().toString();

        NatsEventBus.Subscription[] gameEndedSub = new NatsEventBus.Subscription[1];
        gameEndedSub[0] = bus.subscribe(NatsSubjects.gameEnded(gameId), GameEnded.class, event -> {
            shardCapacityRegistry.onGameEnded(shardId);
            gameEndedSub[0].unsubscribe();
        });

        bus.publish(NatsSubjects.shardAssign(shardId),
                new MatchAssignment(gameId, matchFound.whiteUsername(), matchFound.blackUsername()));

        sendRoleAndSession(gameId, matchFound.whiteUsername(), Piece.Color.WHITE);
        sendRoleAndSession(gameId, matchFound.blackUsername(), Piece.Color.BLACK);
    }

    private void sendRoleAndSession(String gameId, String username, Piece.Color color) {
        ParticipantRole role = color == Piece.Color.WHITE ? ParticipantRole.WHITE : ParticipantRole.BLACK;
        String roleJson = MessageSerializer.serialize(ServerMessageType.ROLE, new RoleMessage(role, color));
        bus.publish(NatsSubjects.clientOutbox(username), roleJson);
        bus.publish(NatsSubjects.sessionAssigned(username), new SessionAssignment(gameId, color));
    }
}
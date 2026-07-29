package com.chessgame.server.matchmaker;

import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.response.ErrorCode;
import com.chessgame.common.protocol.response.ErrorMessage;
import com.chessgame.common.protocol.response.ParticipantRole;
import com.chessgame.common.protocol.response.RoleMessage;
import com.chessgame.common.protocol.response.RoomCancelledMessage;
import com.chessgame.common.protocol.response.RoomCreatedMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.server.bus.MatchFound;
import com.chessgame.server.bus.MatchmakingCancel;
import com.chessgame.server.bus.MatchmakingRequest;
import com.chessgame.server.bus.NatsEventBus;
import com.chessgame.server.bus.NatsSubjects;
import com.chessgame.server.bus.RoomCancelRequest;
import com.chessgame.server.bus.RoomCreateRequest;
import com.chessgame.server.bus.RoomJoinRequest;
import com.chessgame.server.network.MessageSerializer;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MatchmakerRoomsController {

    private static final long PLAY_TIMEOUT_MS = 60_000;

    private final PlayMatchmaker playMatchmaker;
    private final RoomManager roomManager;
    private final NatsEventBus bus;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public MatchmakerRoomsController(PlayMatchmaker playMatchmaker, RoomManager roomManager, NatsEventBus bus) {
        this.playMatchmaker = playMatchmaker;
        this.roomManager = roomManager;
        this.bus = bus;
    }

    public void start() {
        bus.subscribe(NatsSubjects.matchmakingRequest(), MatchmakingRequest.class, this::onMatchmakingRequest);
        bus.subscribe(NatsSubjects.matchmakingCancel(), MatchmakingCancel.class, this::onMatchmakingCancel);
        bus.subscribe(NatsSubjects.roomsCreate(), RoomCreateRequest.class, this::onRoomCreate);
        bus.subscribe(NatsSubjects.roomsJoin(), RoomJoinRequest.class, this::onRoomJoin);
        bus.subscribe(NatsSubjects.roomsCancel(), RoomCancelRequest.class, this::onRoomCancel);
        scheduler.scheduleAtFixedRate(this::checkExpiredMatchmaking, 5, 5, TimeUnit.SECONDS);
    }

    private void onMatchmakingRequest(MatchmakingRequest request) {
        PlayMatchmaker.PairResult result = playMatchmaker.tryPair(request.username(), request.rating());
        if (result instanceof PlayMatchmaker.Paired paired) {
            bus.publish(NatsSubjects.matchFound(), new MatchFound(paired.whiteUsername(), paired.blackUsername()));
        }
    }

    private void onMatchmakingCancel(MatchmakingCancel cancel) {
        playMatchmaker.cancel(cancel.username());
        sendError(cancel.username(), ErrorCode.NO_OPPONENT_FOUND, "Cancelled");
    }

    private void checkExpiredMatchmaking() {
        List<String> expired = playMatchmaker.removeExpiredWaiters(PLAY_TIMEOUT_MS);
        for (String username : expired) {
            sendError(username, ErrorCode.NO_OPPONENT_FOUND, "No opponent found within 1 minute");
        }
    }

    private void onRoomCreate(RoomCreateRequest request) {
        boolean created = roomManager.create(request.roomName(), request.hostUsername());
        if (created) {
            sendToClient(request.hostUsername(), ServerMessageType.ROOM_CREATED,
                    new RoomCreatedMessage(request.roomName()));
        } else {
            sendError(request.hostUsername(), ErrorCode.ROOM_ALREADY_EXISTS, "Room name already taken");
        }
    }

    private void onRoomJoin(RoomJoinRequest request) {
        RoomManager.JoinResult result = roomManager.join(request.roomName(), request.username());

        if (result instanceof RoomManager.Paired paired) {
            bus.publish(NatsSubjects.matchFound(), new MatchFound(paired.hostUsername(), paired.joinerUsername()));
        } else if (result instanceof RoomManager.JoinedAsSpectator) {
            sendToClient(request.username(), ServerMessageType.ROLE, new RoleMessage(ParticipantRole.SPECTATOR, null));
        } else if (result instanceof RoomManager.NotFound) {
            sendError(request.username(), ErrorCode.ROOM_NOT_FOUND, "Room not found");
        }
    }

    private void onRoomCancel(RoomCancelRequest request) {
        roomManager.cancel(request.roomName());
    }

    private void sendError(String username, ErrorCode code, String message) {
        sendToClient(username, ServerMessageType.ERROR, new ErrorMessage(code, message));
    }

    private void sendToClient(String username, ServerMessageType type, Object payload) {
        String json = MessageSerializer.serialize(type, payload);
        bus.publish(NatsSubjects.clientOutbox(username), json);
    }
}
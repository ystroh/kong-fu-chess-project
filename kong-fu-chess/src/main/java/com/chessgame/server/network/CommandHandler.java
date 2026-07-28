package com.chessgame.server.network;

import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.request.CancelPlayMessage;
import com.chessgame.common.protocol.request.CancelRoomMessage;
import com.chessgame.common.protocol.request.CreateRoomMessage;
import com.chessgame.common.protocol.request.JoinRoomMessage;
import com.chessgame.common.protocol.request.JumpMessage;
import com.chessgame.common.protocol.request.LoginMessage;
import com.chessgame.common.protocol.request.MessageType;
import com.chessgame.common.protocol.request.MoveMessage;
import com.chessgame.common.protocol.request.RegisterMessage;
import com.chessgame.common.protocol.request.ResignMessage;
import com.chessgame.common.protocol.response.AuthOkMessage;
import com.chessgame.common.protocol.response.ErrorCode;
import com.chessgame.common.protocol.response.ErrorMessage;
import com.chessgame.common.protocol.response.ParticipantRole;
import com.chessgame.common.protocol.response.ResumeMessage;
import com.chessgame.common.protocol.response.RoleMessage;
import com.chessgame.common.protocol.response.RoomCancelledMessage;
import com.chessgame.common.protocol.response.RoomCreatedMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.server.Command;
import com.chessgame.server.CommandParser;
import com.chessgame.server.ConnectionSession;
import com.chessgame.server.bus.*;
import com.chessgame.server.repository.PasswordHasher;
import com.chessgame.server.repository.UserRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class CommandHandler {

    private final Gson gson = new Gson();
    private final CommandParser commandParser = new CommandParser();
    private final ClientGateway gateway;
    private final UserRepository userRepository;
    private final NatsEventBus bus;

    public CommandHandler(UserRepository userRepository, LocalClientGateway gateway, NatsEventBus bus){
        this.userRepository = userRepository;
        this.gateway = gateway;
        this.bus = bus;
    }




    public void handle(ConnectionSession session, String rawMessage) {
        JsonObject json = JsonParser.parseString(rawMessage).getAsJsonObject();
        MessageType type = MessageType.valueOf(json.get("type").getAsString());

        switch (type) {
            case LOGIN -> handleLogin(session, gson.fromJson(json, LoginMessage.class));
            case REGISTER -> handleRegister(session, gson.fromJson(json, RegisterMessage.class));
            case PLAY -> handlePlay(session);
            case CANCEL_PLAY -> handleCancelPlay(session, gson.fromJson(json, CancelPlayMessage.class));
            case CREATE_ROOM -> handleCreateRoom(session, gson.fromJson(json, CreateRoomMessage.class));
            case JOIN_ROOM -> handleJoinRoom(session, gson.fromJson(json, JoinRoomMessage.class));
            case CANCEL_ROOM -> handleCancelRoom(session, gson.fromJson(json, CancelRoomMessage.class));
            case MOVE -> handleMove(session, gson.fromJson(json, MoveMessage.class));
            case JUMP -> handleJump(session, gson.fromJson(json, JumpMessage.class));
            case RESIGN -> handleResign(session, gson.fromJson(json, ResignMessage.class));
        }
    }

    public void handleDisconnect(ConnectionSession session) {
        if (session.state() == ConnectionSession.State.IN_GAME) {
            bus.publish("reconnection.disconnect", new DisconnectNotice(session.username(), session.gameId(), session.color()));
        }
    }

    private void handleLogin(ConnectionSession session, LoginMessage msg) {
        if (session.state() != ConnectionSession.State.OPEN) return;

        var user = userRepository.findByUsername(msg.username());
        if (user.isEmpty()) {
            session.send(ServerMessageType.ERROR, new ErrorMessage(ErrorCode.USER_NOT_FOUND, "User not found"));
            return;
        }
        if (!PasswordHasher.hash(msg.password()).equals(user.get().passwordHash())) {
            session.send(ServerMessageType.ERROR, new ErrorMessage(ErrorCode.WRONG_PASSWORD, "Wrong password"));
            return;
        }

        session.setUsername(msg.username());
        session.setRating(user.get().rating());
        gateway.register(msg.username(), session.connection());

        bus.publish("reconnection.attempt", new ReconnectAttempt(msg.username()));

        session.send(ServerMessageType.AUTH_OK, new AuthOkMessage(msg.username()));
        session.setState(ConnectionSession.State.AUTHENTICATED);
    }

    private void handleRegister(ConnectionSession session, RegisterMessage msg) {
        if (session.state() != ConnectionSession.State.OPEN) return;

        if (userRepository.findByUsername(msg.username()).isPresent()) {
            session.send(ServerMessageType.ERROR, new ErrorMessage(ErrorCode.USERNAME_TAKEN, "Username already taken"));
            return;
        }

        userRepository.create(msg.username(), msg.password());
        session.setUsername(msg.username());
        session.setRating(UserRepository.STARTING_RATING);
        gateway.register(msg.username(), session.connection());

        session.send(ServerMessageType.AUTH_OK, new AuthOkMessage(msg.username()));
        session.setState(ConnectionSession.State.AUTHENTICATED);
    }

    private void handlePlay(ConnectionSession session) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        bus.publish("matchmaking.request", new MatchmakingRequest(session.username(), session.rating()));
    }

    private void handleCancelPlay(ConnectionSession session, CancelPlayMessage msg) {
        bus.publish("matchmaking.cancel", new MatchmakingCancel(session.username()));
    }

    private void handleCreateRoom(ConnectionSession session, CreateRoomMessage msg) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        bus.publish("rooms.create", new RoomCreateRequest(msg.roomName(), session.username()));
        session.send(ServerMessageType.ROOM_CREATED, new RoomCreatedMessage(msg.roomName()));
    }

    private void handleJoinRoom(ConnectionSession session, JoinRoomMessage msg) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        bus.publish("rooms.join", new RoomJoinRequest(msg.roomName(), session.username()));
    }

    private void handleCancelRoom(ConnectionSession session, CancelRoomMessage msg) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        bus.publish("rooms.cancel", new RoomCancelRequest(msg.roomName()));
        session.send(ServerMessageType.ROOM_CANCELLED, new RoomCancelledMessage(msg.roomName()));
    }

    private void handleMove(ConnectionSession session, MoveMessage msg) {
        if (session.state() != ConnectionSession.State.IN_GAME) return;
        Command command = commandParser.parseMove(msg, session.color());
        bus.publish("game." + session.gameId() + ".commands", CommandEnvelope.of(command));
    }

    private void handleJump(ConnectionSession session, JumpMessage msg) {
        if (session.state() != ConnectionSession.State.IN_GAME) return;
        Command command = commandParser.parseJump(msg, session.color());
        bus.publish("game." + session.gameId() + ".commands", CommandEnvelope.of(command));
    }

    private void handleResign(ConnectionSession session, ResignMessage msg) {
        if (session.state() != ConnectionSession.State.IN_GAME) return;
        bus.publish("game." + session.gameId() + ".commands", CommandEnvelope.of(new Command.Resign(session.color())));
    }
}
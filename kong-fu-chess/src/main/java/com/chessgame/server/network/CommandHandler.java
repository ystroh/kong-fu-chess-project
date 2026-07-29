package com.chessgame.server.network;

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
import com.chessgame.common.protocol.response.RoomCancelledMessage;
import com.chessgame.common.protocol.response.RoomCreatedMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.server.Command;
import com.chessgame.server.CommandParser;
import com.chessgame.server.ConnectionSession;
import com.chessgame.server.bus.CommandEnvelope;
import com.chessgame.server.bus.MatchmakingCancel;
import com.chessgame.server.bus.MatchmakingRequest;
import com.chessgame.server.bus.NatsEventBus;
import com.chessgame.server.bus.NatsSubjects;
import com.chessgame.server.bus.PlayerDisconnected;
import com.chessgame.server.bus.ReconnectInfo;
import com.chessgame.server.bus.RoomCancelRequest;
import com.chessgame.server.bus.RoomCreateRequest;
import com.chessgame.server.bus.RoomJoinRequest;
import com.chessgame.server.redis.RedisClient;
import com.chessgame.server.repository.PasswordHasher;
import com.chessgame.server.repository.UserRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import redis.clients.jedis.Jedis;

public final class CommandHandler {

    private final Gson gson = new Gson();
    private final CommandParser commandParser = new CommandParser();
    private final UserRepository userRepository;
    private final NatsEventBus bus;
    private final SessionRegistry sessionRegistry;

    public CommandHandler(UserRepository userRepository, NatsEventBus bus, SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.bus = bus;
        this.sessionRegistry = sessionRegistry;
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
        if (session.state() != ConnectionSession.State.IN_GAME) return;

        try (Jedis jedis = RedisClient.pool().getResource()) {
            String key = NatsSubjects.reconnectKey(session.username());
            jedis.set(key, gson.toJson(new ReconnectInfo(session.gameId(), session.color())));
            jedis.expire(key, 30);
        }

        bus.publish(NatsSubjects.playerDisconnected(session.gameId()), new PlayerDisconnected(session.color()));
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
        sessionRegistry.register(msg.username(), session);

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
        sessionRegistry.register(msg.username(), session);

        session.send(ServerMessageType.AUTH_OK, new AuthOkMessage(msg.username()));
        session.setState(ConnectionSession.State.AUTHENTICATED);
    }

    private void handlePlay(ConnectionSession session) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        bus.publish(NatsSubjects.matchmakingRequest(), new MatchmakingRequest(session.username(), session.rating()));
    }

    private void handleCancelPlay(ConnectionSession session, CancelPlayMessage msg) {
        bus.publish(NatsSubjects.matchmakingCancel(), new MatchmakingCancel(session.username()));
    }

    private void handleCreateRoom(ConnectionSession session, CreateRoomMessage msg) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        bus.publish(NatsSubjects.roomsCreate(), new RoomCreateRequest(msg.roomName(), session.username()));
        session.send(ServerMessageType.ROOM_CREATED, new RoomCreatedMessage(msg.roomName()));
    }

    private void handleJoinRoom(ConnectionSession session, JoinRoomMessage msg) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        bus.publish(NatsSubjects.roomsJoin(), new RoomJoinRequest(msg.roomName(), session.username()));
    }

    private void handleCancelRoom(ConnectionSession session, CancelRoomMessage msg) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        bus.publish(NatsSubjects.roomsCancel(), new RoomCancelRequest(msg.roomName()));
        session.send(ServerMessageType.ROOM_CANCELLED, new RoomCancelledMessage(msg.roomName()));
    }

    private void handleMove(ConnectionSession session, MoveMessage msg) {
        if (session.state() != ConnectionSession.State.IN_GAME) return;
        publishCommand(session, commandParser.parseMove(msg, session.color()));
    }

    private void handleJump(ConnectionSession session, JumpMessage msg) {
        if (session.state() != ConnectionSession.State.IN_GAME) return;
        publishCommand(session, commandParser.parseJump(msg, session.color()));
    }

    private void handleResign(ConnectionSession session, ResignMessage msg) {
        if (session.state() != ConnectionSession.State.IN_GAME) return;
        publishCommand(session, new Command.Resign(session.color()));
    }

    private void publishCommand(ConnectionSession session, Command command) {
        bus.publish(NatsSubjects.commands(session.gameId()), CommandEnvelope.of(command));
    }
}
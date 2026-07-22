package com.chessgame.server.application;

import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.request.*;
import com.chessgame.server.Command;
import com.chessgame.server.network.CommandParser;
import com.chessgame.server.network.ConnectionSession;
import com.chessgame.server.GameMatch;
import com.chessgame.server.PlayerConnection;
import com.chessgame.server.repository.PasswordHasher;
import com.chessgame.server.repository.UserRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class CommandHandler {

    private final Gson gson = new Gson();
    private final CommandParser commandParser = new CommandParser();
    private final PlayMatchmaker playMatchmaker = new PlayMatchmaker();
    private final RoomManager roomManager = new RoomManager();
    private final ReconnectionManager reconnectionManager = new ReconnectionManager();
    private final UserRepository userRepository;
    private final MatchLauncher matchLauncher;

    public CommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.matchLauncher = new MatchLauncher(userRepository);
    }

    public void handle(ConnectionSession session, String rawMessage) {
        JsonObject json = JsonParser.parseString(rawMessage).getAsJsonObject();
        MessageType type = MessageType.valueOf(json.get("type").getAsString());

        switch (type) {
            case LOGIN -> handleLogin(session, gson.fromJson(json, LoginMessage.class));
            case REGISTER -> handleRegister(session, gson.fromJson(json, RegisterMessage.class));
            case PLAY -> handlePlay(session);
            case CREATE_ROOM -> handleCreateRoom(session, gson.fromJson(json, CreateRoomMessage.class));
            case JOIN_ROOM -> handleJoinRoom(session, gson.fromJson(json, JoinRoomMessage.class));
            case CANCEL_ROOM -> handleCancelRoom(session, gson.fromJson(json, CancelRoomMessage.class));
            case MOVE -> handleMove(session, gson.fromJson(json, MoveMessage.class));
            case JUMP -> handleJump(session, gson.fromJson(json, JumpMessage.class));
        }
    }

    public void handleDisconnect(ConnectionSession session) {
        if (session.state() == ConnectionSession.State.IN_GAME) {
            reconnectionManager.handleDisconnect(session);
        }
    }

    private void handleLogin(ConnectionSession session, LoginMessage msg) {
        if (session.state() != ConnectionSession.State.OPEN) return;

        var user = userRepository.findByUsername(msg.username());
        if (user.isEmpty()) {
            session.send("AUTH_FAIL:User not found");
            return;
        }
        if (!PasswordHasher.hash(msg.password()).equals(user.get().passwordHash())) {
            session.send("AUTH_FAIL:Wrong password");
            return;
        }

        session.setUsername(msg.username());
        session.setRating(user.get().rating());

        if (reconnectionManager.tryReconnect(msg.username(), session)) {
            Piece.Color color = colorOf(session);
            session.send("RESUME:" + color);
            return;
        }

        session.send("AUTH_OK:" + msg.username());
        session.setState(ConnectionSession.State.AUTHENTICATED);
    }

    private void handleRegister(ConnectionSession session, RegisterMessage msg) {
        if (session.state() != ConnectionSession.State.OPEN) return;

        if (userRepository.findByUsername(msg.username()).isPresent()) {
            session.send("AUTH_FAIL:Username already taken");
            return;
        }

        userRepository.create(msg.username(), msg.password());
        session.send("AUTH_OK:" + msg.username());
        session.setUsername(msg.username());
        session.setRating(UserRepository.STARTING_RATING);
        session.setState(ConnectionSession.State.AUTHENTICATED);
    }

    private void handlePlay(ConnectionSession session) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;

        PlayMatchmaker.Pairing pairing = playMatchmaker.tryPair(session);
        if (pairing == null) return;

        String gameId = "game-" + System.currentTimeMillis();
        GameMatch match = matchLauncher.launch(gameId, pairing.white(), pairing.black(),
                pairing.whiteSession().username(), pairing.blackSession().username());

        updateSessionForGame(pairing.whiteSession(), pairing.white(), match);
        updateSessionForGame(pairing.blackSession(), pairing.black(), match);
        pairing.white().send("ROLE:WHITE");
        pairing.black().send("ROLE:BLACK");
    }

    private void handleCreateRoom(ConnectionSession session, CreateRoomMessage msg) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        roomManager.create(msg.roomName(), session);
        session.send("ROOM_CREATED:" + msg.roomName());
    }

    private void handleJoinRoom(ConnectionSession session, JoinRoomMessage msg) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;

        RoomManager.JoinResult result = roomManager.join(msg.roomName(), session);
        if (result instanceof RoomManager.Paired paired) {
            String gameId = "game-" + System.currentTimeMillis();
            GameMatch match = matchLauncher.launch(gameId, paired.white(), paired.black(),
                    paired.whiteSession().username(), paired.blackSession().username());
            roomManager.attachMatch(msg.roomName(), match);

            updateSessionForGame(paired.whiteSession(), paired.white(), match);
            updateSessionForGame(paired.blackSession(), paired.black(), match);
            paired.white().send("ROLE:WHITE");
            paired.black().send("ROLE:BLACK");
        } else if (result instanceof RoomManager.NotFound) {
            session.send("ROOM_NOT_FOUND");
        } else if (result instanceof RoomManager.JoinedAsSpectator) {
            session.send("ROLE:SPECTATOR");
        }
    }

    private void handleCancelRoom(ConnectionSession session, CancelRoomMessage msg) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        roomManager.cancel(msg.roomName());
        session.send("ROOM_CANCELLED:" + msg.roomName());
    }

    private void handleMove(ConnectionSession session, MoveMessage msg) {
        if (session.state() != ConnectionSession.State.IN_GAME) return;
        Command command = commandParser.parseMove(msg, colorOf(session));
        session.match().submitCommand(command);
    }

    private void handleJump(ConnectionSession session, JumpMessage msg) {
        if (session.state() != ConnectionSession.State.IN_GAME) return;
        Command command = commandParser.parseJump(msg, colorOf(session));
        session.match().submitCommand(command);
    }

    private Piece.Color colorOf(ConnectionSession session) {
        return session.playerConnection().role() == PlayerConnection.Role.WHITE
                ? Piece.Color.WHITE : Piece.Color.BLACK;
    }

    private void updateSessionForGame(ConnectionSession session, PlayerConnection playerConnection, GameMatch match) {
        session.setPlayerConnection(playerConnection);
        session.setMatch(match);
        session.setState(ConnectionSession.State.IN_GAME);
    }
}
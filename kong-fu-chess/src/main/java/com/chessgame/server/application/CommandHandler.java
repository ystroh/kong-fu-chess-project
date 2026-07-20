package com.chessgame.server.application;

import com.chessgame.common.model.Piece;
import com.chessgame.server.Command;
import com.chessgame.server.GameMatch;
import com.chessgame.server.PlayerConnection;
import com.chessgame.server.network.CommandParser;
import com.chessgame.server.network.ConnectionSession;

public final class CommandHandler {

    private final CommandParser commandParser = new CommandParser();
    private final PlayMatchmaker playMatchmaker = new PlayMatchmaker();
    private final RoomManager roomManager = new RoomManager();
    private final MatchLauncher matchLauncher = new MatchLauncher();

    public void handle(ConnectionSession session, ServerSocketConnection connection, String message) {
        String[] parts = message.trim().split("\\s+", 2);
        String type = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (type) {
            case "REGISTER" -> handleRegister(session, args);
            case "LOGIN" -> handleLogin(session, args);
            case "PLAY" -> handlePlay(session, connection);
            case "CREATE_ROOM" -> handleCreateRoom(session, connection, args);
            case "JOIN_ROOM" -> handleJoinRoom(session, connection, args);
            case "CANCEL_ROOM" -> handleCancelRoom(session, args);
            case "MOVE", "JUMP" -> handleGameCommand(session, message);
            default -> { }
        }
    }

    private void handleRegister(ConnectionSession session, String args) {
        if (session.state() != ConnectionSession.State.OPEN) {
            return;
        }
        // TODO: קליטת שם-משתמש+סיסמה, שמירה ב-SQLite, session.setState(AUTHENTICATED)
    }

    private void handleLogin(ConnectionSession session, String args) {
        if (session.state() != ConnectionSession.State.OPEN) {
            return;
        }
        // TODO: אימות מול SQLite, session.setState(AUTHENTICATED)
    }


    private void handlePlay(ConnectionSession session, ServerSocketConnection connection) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;

        PlayMatchmaker.Pairing pairing = playMatchmaker.tryPair(session, connection);
        if (pairing == null) return;

        String gameId = "game-" + System.currentTimeMillis();
        GameMatch match = matchLauncher.launch(gameId, pairing.white(), pairing.black());

        pairing.white().send("ROLE:WHITE");
        pairing.black().send("ROLE:BLACK");

        updateSessionForGame(pairing.whiteSession(), pairing.white(), match);
        updateSessionForGame(pairing.blackSession(), pairing.black(), match);
    }

    private void handleCreateRoom(ConnectionSession session, ServerSocketConnection connection, String roomName) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;
        roomManager.create(roomName, session, connection);
    }

    private void handleJoinRoom(ConnectionSession session, ServerSocketConnection connection, String roomName) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) return;

        RoomManager.JoinResult result = roomManager.join(roomName, connection);
        if (result instanceof RoomManager.Paired paired) {
            String gameId = "game-" + System.currentTimeMillis();
            GameMatch match = matchLauncher.launch(gameId, paired.white(), paired.black());
            roomManager.attachMatch(roomName, match);

            paired.white().send("ROLE:WHITE");
            paired.black().send("ROLE:BLACK");

            updateSessionForGame(paired.hostSession(), paired.white(), match);
            updateSessionForGame(session, paired.black(), match);
        }
        // TODO: NotFound / JoinedAsSpectator - הודעה-חוזרת ללקוח
    }

    private void updateSessionForGame(ConnectionSession session, PlayerConnection playerConnection, GameMatch match) {
        session.setPlayerConnection(playerConnection);
        session.setMatch(match);
        session.setState(ConnectionSession.State.IN_GAME);
    }
    private void handleCancelRoom(ConnectionSession session, String roomName) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) {
            return;
        }
        roomManager.cancel(roomName);
    }

    private void handleGameCommand(ConnectionSession session, String message) {
        if (session.state() != ConnectionSession.State.IN_GAME) {
            return;
        }
        Piece.Color color = session.playerConnection().role() == PlayerConnection.Role.WHITE
                ? Piece.Color.WHITE : Piece.Color.BLACK;
        Command command = commandParser.parse(message, color);
        if (command != null) {
            session.match().submitCommand(command);
        }
    }
}
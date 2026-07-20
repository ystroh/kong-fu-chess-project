package com.chessgame.server.network;

import com.chessgame.common.model.Piece;
import com.chessgame.server.Command;
import com.chessgame.server.network.CommandParser;
import com.chessgame.server.network.ConnectionSession;
import com.chessgame.server.GameMatch;
import com.chessgame.server.network.JavaWebSocketConnection;
import com.chessgame.server.PlayerConnection;
import com.chessgame.server.network.ServerSocketConnection;
import com.chessgame.server.application.MatchLauncher;
import com.chessgame.server.application.PlayMatchmaker;
import com.chessgame.server.application.RoomManager;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public final class ChessWebSocketServer extends WebSocketServer {

    private final Map<WebSocket, ConnectionSession> sessions = new HashMap<>();
    private final CommandParser commandParser = new CommandParser();
    private final PlayMatchmaker playMatchmaker = new PlayMatchmaker();
    private final RoomManager roomManager = new RoomManager();
    private final MatchLauncher matchLauncher = new MatchLauncher();

    public ChessWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        sessions.put(conn, new ConnectionSession());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ConnectionSession session = sessions.get(conn);
        if (session == null) {
            return;
        }

        String[] parts = message.trim().split("\\s+", 2);
        String type = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (type) {
            case "REGISTER" -> handleRegister(conn, session, args);
            case "LOGIN" -> handleLogin(conn, session, args);
            case "PLAY" -> handlePlay(conn, session);
            case "CREATE_ROOM" -> handleCreateRoom(conn, session, args);
            case "JOIN_ROOM" -> handleJoinRoom(conn, session, args);
            case "CANCEL_ROOM" -> handleCancelRoom(conn, session, args);
            case "MOVE", "JUMP" -> handleGameCommand(session, message);
            default -> {
            }
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        sessions.remove(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started");
    }

    private void handleRegister(WebSocket conn, ConnectionSession session, String args) {
        if (session.state() != ConnectionSession.State.OPEN) {
            return;
        }
        // TODO: קליטת שם-משתמש+סיסמה, שמירה ב-SQLite, session.setState(AUTHENTICATED)
    }

    private void handleLogin(WebSocket conn, ConnectionSession session, String args) {
        if (session.state() != ConnectionSession.State.OPEN) {
            return;
        }
        // TODO: אימות מול SQLite, session.setState(AUTHENTICATED)
    }

    private void handlePlay(WebSocket conn, ConnectionSession session) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) {
            return;
        }

        ServerSocketConnection wrapped = new JavaWebSocketConnection(conn);
        PlayMatchmaker.Pairing pairing = playMatchmaker.tryPair(wrapped);
        if (pairing == null) {
            return;
        }

        String gameId = "game-" + System.currentTimeMillis();
        GameMatch match = matchLauncher.launch(gameId, pairing.white(), pairing.black());
        // TODO: לעדכן את session (ואת ה-session של היריב!) ל-IN_GAME, playerConnection, match
    }

    private void handleCreateRoom(WebSocket conn, ConnectionSession session, String roomName) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) {
            return;
        }
        roomManager.create(roomName, new JavaWebSocketConnection(conn));
    }

    private void handleJoinRoom(WebSocket conn, ConnectionSession session, String roomName) {
        if (session.state() != ConnectionSession.State.AUTHENTICATED) {
            return;
        }

        RoomManager.JoinResult result = roomManager.join(roomName, new JavaWebSocketConnection(conn));
        if (result instanceof RoomManager.Paired paired) {
            String gameId = "game-" + System.currentTimeMillis();
            GameMatch match = matchLauncher.launch(gameId, paired.white(), paired.black());
            roomManager.attachMatch(roomName, match);
            // TODO: כמו ב-handlePlay - עדכון session-ים ל-IN_GAME
        }
        // TODO: NotFound / JoinedAsSpectator - הודעה-חוזרת ללקוח
    }

    private void handleCancelRoom(WebSocket conn, ConnectionSession session, String roomName) {
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
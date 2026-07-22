package com.chessgame.client.ui;

import com.chessgame.client.input.BoardMapper;
import com.chessgame.client.input.Controller;
import com.chessgame.client.input.RemoteMoveChannel;
import com.chessgame.client.network.ServerGateway;
import com.chessgame.common.engine.GameSnapshot;
import com.chessgame.common.engine.MoveChannel;
import com.chessgame.common.model.Board;
import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.response.GameStateMessage;
import com.chessgame.common.protocol.response.OpponentDisconnectedMessage;
import com.chessgame.common.protocol.response.OpponentReconnectedMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class GameStateCoordinator {

    private final ServerGateway gateway;
    private final Piece.Color myColor;
    private final Gson gson = new Gson();
    private final List<SnapshotListener> listeners = new CopyOnWriteArrayList<>();
    private final MoveChannel moveChannel;

    private volatile GameSnapshot lastSnapshot;
    private boolean controllerReady = false;
    private Runnable disconnectCallback;
    private Consumer<Controller> onControllerReady;
    private Consumer<String> onOpponentStatusChanged;

    public GameStateCoordinator(ServerGateway gateway, Piece.Color myColor) {
        this.gateway = gateway;
        this.myColor = myColor;
        this.moveChannel = new RemoteMoveChannel(gateway, this);
    }

    public void subscribeToServer() {
        gateway.subscribe(this, ServerMessageType.GAME_STATE, this::handleGameState);
        gateway.subscribe(this, ServerMessageType.OPPONENT_DISCONNECTED, this::handleOpponentDisconnected);
        gateway.subscribe(this, ServerMessageType.OPPONENT_RECONNECTED, this::handleOpponentReconnected);
    }

    public void addListener(SnapshotListener listener) {
        listeners.add(listener);
    }

    public void onControllerReady(Consumer<Controller> callback) {
        this.onControllerReady = callback;
    }

    public void onOpponentStatusChanged(Consumer<String> callback) {
        this.onOpponentStatusChanged = callback;
    }

    public void setDisconnectCallback(Runnable callback) {
        this.disconnectCallback = callback;
        gateway.setDisconnectListener(this::onDisconnected);
    }

    public GameSnapshot currentSnapshot() {
        return lastSnapshot;
    }

    private void onDisconnected() {
        if (disconnectCallback != null) {
            disconnectCallback.run();
        }
    }

    private void handleGameState(JsonObject json) {
        GameStateMessage msg = gson.fromJson(json, GameStateMessage.class);
        lastSnapshot = msg.snapshot();

        if (!controllerReady && myColor != null) {
            Board board = new Board(lastSnapshot.width(), lastSnapshot.height());
            BoardMapper boardMapper = new BoardMapper(board);
            Controller controller = new Controller(boardMapper, moveChannel, myColor);
            controllerReady = true;
            if (onControllerReady != null) {
                onControllerReady.accept(controller);
            }
        }

        SwingUtilities.invokeLater(this::notifyListeners);
    }

    private void handleOpponentDisconnected(JsonObject json) {
        OpponentDisconnectedMessage msg = gson.fromJson(json, OpponentDisconnectedMessage.class);
        SwingUtilities.invokeLater(() -> {
            if (onOpponentStatusChanged != null) {
                onOpponentStatusChanged.accept("היריב התנתק, " + msg.secondsRemaining() + " שניות...");
            }
        });
    }

    private void handleOpponentReconnected(JsonObject json) {
        SwingUtilities.invokeLater(() -> {
            if (onOpponentStatusChanged != null) {
                onOpponentStatusChanged.accept(null);
            }
        });
    }

    private void notifyListeners() {
        for (SnapshotListener listener : listeners) {
            listener.onSnapshotUpdated(lastSnapshot);
        }
    }
}
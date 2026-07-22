package com.chessgame.client;

import com.chessgame.client.network.ServerConnection;
import com.chessgame.client.network.ServerGateway;
import com.chessgame.client.network.WebSocketServerConnection;
import com.chessgame.client.ui.AuthScreen;
import com.chessgame.client.ui.GameStateCoordinator;
import com.chessgame.client.ui.GameWindow;
import com.chessgame.client.ui.MenuScreen;
import com.chessgame.common.model.Piece;

public final class ClientApp {

    private static final String SERVER_URL = "ws://localhost:8887";

    private String savedUsername;
    private String savedPassword;
    private ServerGateway activeGateway;

    public void start() {
        ServerConnection connection = new WebSocketServerConnection(SERVER_URL);
        ServerGateway gateway = new ServerGateway(connection);
        showAuthScreen(gateway);
    }

    private void showAuthScreen(ServerGateway gateway) {
        AuthScreen authScreen = new AuthScreen(gateway,
                (username, password) -> {
                    savedUsername = username;
                    savedPassword = password;
                    showMenu(gateway, username);
                },
                (color, username) -> {
                    savedUsername = username;
                    startGame(gateway, color, username);
                });
        authScreen.setVisible(true);
    }

    private void showMenu(ServerGateway gateway, String username) {
        MenuScreen menuScreen = new MenuScreen(gateway, username, color -> startGame(gateway, color, username));
        menuScreen.setVisible(true);
    }

    private void startGame(ServerGateway gateway, Piece.Color myColor, String username) {
        this.activeGateway = gateway;
        String whiteName = myColor == Piece.Color.WHITE ? username : "Opponent";
        String blackName = myColor == Piece.Color.BLACK ? username : "Opponent";

        GameStateCoordinator coordinator = new GameStateCoordinator(gateway, myColor);
        coordinator.setDisconnectCallback(this::attemptReconnect);

        GameWindow gameWindow = new GameWindow(coordinator, whiteName, blackName);
        gameWindow.init();
    }

    private void attemptReconnect() {
        ServerConnection newConnection = new WebSocketServerConnection(SERVER_URL);
        activeGateway.updateConnection(newConnection);
        activeGateway.login(savedUsername, savedPassword);
    }

    public static void main(String[] args) {
        new ClientApp().start();
    }
}
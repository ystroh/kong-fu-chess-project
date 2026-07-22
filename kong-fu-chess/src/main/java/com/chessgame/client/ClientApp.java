package com.chessgame.client;

import com.chessgame.client.network.ServerConnection;
import com.chessgame.client.network.ServerGateway;
import com.chessgame.client.network.WebSocketServerConnection;
import com.chessgame.client.ui.AuthScreen;
import com.chessgame.client.ui.GameWindow;
import com.chessgame.client.ui.MenuScreen;
import com.chessgame.common.model.Piece;

public final class ClientApp {

    private static final String SERVER_URL = "ws://localhost:8887";

    private String savedUsername;
    private String savedPassword;
    private GameWindow activeGameWindow;

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
        String whiteName = myColor == Piece.Color.WHITE ? username : "Opponent";
        String blackName = myColor == Piece.Color.BLACK ? username : "Opponent";

        activeGameWindow = new GameWindow(gateway, myColor, whiteName, blackName);
        activeGameWindow.setDisconnectCallback(() -> attemptReconnect(gateway));
        activeGameWindow.init();
    }

    private void attemptReconnect(ServerGateway gateway) {
        ServerConnection newConnection = new WebSocketServerConnection(SERVER_URL);
        gateway.updateConnection(newConnection);
        gateway.login(savedUsername, savedPassword);
    }

    public static void main(String[] args) {
        new ClientApp().start();
    }
}
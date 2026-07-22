package com.chessgame.client;

import com.chessgame.client.network.ServerConnection;
import com.chessgame.client.network.WebSocketServerConnection;
import com.chessgame.client.ui.AuthScreen;
import com.chessgame.client.ui.GameWindow;
import com.chessgame.client.ui.MenuScreen;
import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.request.LoginMessage;
import com.chessgame.common.protocol.request.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public final class ClientApp {

    private static final String SERVER_URL = "ws://localhost:8887";

    private final Gson gson = new Gson();
    private String savedUsername;
    private String savedPassword;
    private GameWindow activeGameWindow;

    public void start() {
        ServerConnection connection = new WebSocketServerConnection(SERVER_URL);
        showAuthScreen(connection);
    }

    private void showAuthScreen(ServerConnection connection) {
        AuthScreen authScreen = new AuthScreen(connection,
                (username, password) -> {
                    savedUsername = username;
                    savedPassword = password;
                    showMenu(connection, username);
                },
                (color, username) -> {
                    savedUsername = username;
                    startGame(connection, color, username);
                });
        authScreen.setVisible(true);
    }

    private void showMenu(ServerConnection connection, String username) {
        MenuScreen menuScreen = new MenuScreen(connection, username, color -> startGame(connection, color, username));
        menuScreen.setVisible(true);
    }

    private void startGame(ServerConnection connection, Piece.Color myColor, String username) {
        String whiteName = myColor == Piece.Color.WHITE ? username : "Opponent";
        String blackName = myColor == Piece.Color.BLACK ? username : "Opponent";

        activeGameWindow = new GameWindow(connection, myColor, whiteName, blackName);
        activeGameWindow.setDisconnectCallback(this::attemptReconnect);
        activeGameWindow.init();
    }

    private void attemptReconnect() {
        ServerConnection newConnection = new WebSocketServerConnection(SERVER_URL);
        newConnection.setMessageListener(message -> {
            if (message.startsWith("RESUME:")) {
                activeGameWindow.reconnect(newConnection);
            }
        });

        JsonObject json = gson.toJsonTree(new LoginMessage(savedUsername, savedPassword)).getAsJsonObject();
        json.addProperty("type", MessageType.LOGIN.name());
        newConnection.send(json.toString());
    }

    public static void main(String[] args) {
        new ClientApp().start();
    }
}
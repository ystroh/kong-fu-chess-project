package com.chessgame.client.ui;

import com.chessgame.client.network.ServerConnection;
import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.request.LoginMessage;
import com.chessgame.common.protocol.request.MessageType;
import com.chessgame.common.protocol.request.RegisterMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public final class AuthScreen extends JFrame {
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);

    private final ServerConnection connection;
    private final BiConsumer<String, String> onAuthenticated;
    private final java.util.function.BiConsumer<Piece.Color, String> onResumed;
    private final Gson gson = new Gson();
    private JLabel statusLabel;
    private String pendingUsername;
    private String pendingPassword;

    public AuthScreen(ServerConnection connection,
                      BiConsumer<String, String> onAuthenticated,
                      BiConsumer<Piece.Color, String> onResumed) {
        super("Kong Fu Chess - Login");
        this.connection = connection;
        this.onAuthenticated = onAuthenticated;
        this.onResumed = onResumed;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null);

        connection.setMessageListener(this::handleServerMessage);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.BLACK);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        gbc.gridy = 0;
        form.add(label("שם משתמש:"), gbc);
        gbc.gridy = 1;
        form.add(usernameField, gbc);
        gbc.gridy = 2;
        form.add(label("סיסמה:"), gbc);
        gbc.gridy = 3;
        form.add(passwordField, gbc);

        JButton loginButton = new JButton("התחברות");
        JButton registerButton = new JButton("הרשמה");
        loginButton.addActionListener(e -> send(MessageType.LOGIN, usernameField, passwordField));
        registerButton.addActionListener(e -> send(MessageType.REGISTER, usernameField, passwordField));

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.setBackground(Color.BLACK);
        buttons.add(loginButton);
        buttons.add(registerButton);

        gbc.gridy = 4;
        gbc.insets = new Insets(20, 0, 6, 0);
        form.add(buttons, gbc);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridy = 5;
        form.add(statusLabel, gbc);

        setContentPane(form);
    }

    private void send(MessageType type, JTextField username, JPasswordField password) {
        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        this.pendingUsername = user;
        this.pendingPassword = pass;

        Object payload = type == MessageType.LOGIN
                ? new LoginMessage(user, pass)
                : new RegisterMessage(user, pass);

        JsonObject json = gson.toJsonTree(payload).getAsJsonObject();
        json.addProperty("type", type.name());
        connection.send(json.toString());
    }

    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("RESUME:")) {
                Piece.Color color = Piece.Color.valueOf(message.substring("RESUME:".length()));
                dispose();
                onResumed.accept(color, pendingUsername);
            } else if (message.startsWith("AUTH_OK:")) {
                String username = message.substring("AUTH_OK:".length());
                dispose();
                onAuthenticated.accept(username, pendingPassword);
            } else if (message.startsWith("AUTH_FAIL:")) {
                statusLabel.setText(message.substring("AUTH_FAIL:".length()));
            }
        });
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        return l;
    }
}
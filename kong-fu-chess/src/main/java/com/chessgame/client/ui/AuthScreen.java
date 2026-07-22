package com.chessgame.client.ui;

import com.chessgame.client.network.ServerGateway;
import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.response.AuthOkMessage;
import com.chessgame.common.protocol.response.ErrorMessage;
import com.chessgame.common.protocol.response.ResumeMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public final class AuthScreen extends JFrame {
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);

    private final ServerGateway gateway;
    private final BiConsumer<String, String> onAuthenticated;
    private final BiConsumer<Piece.Color, String> onResumed;
    private final Gson gson = new Gson();
    private JLabel statusLabel;
    private String pendingUsername;
    private String pendingPassword;

    public AuthScreen(ServerGateway gateway,
                      BiConsumer<String, String> onAuthenticated,
                      BiConsumer<Piece.Color, String> onResumed) {
        super("Kong Fu Chess - Login");
        this.gateway = gateway;
        this.onAuthenticated = onAuthenticated;
        this.onResumed = onResumed;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null);

        gateway.subscribe(this, ServerMessageType.AUTH_OK, this::handleAuthOk);
        gateway.subscribe(this, ServerMessageType.ERROR, this::handleError);
        gateway.subscribe(this, ServerMessageType.RESUME, this::handleResume);

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
        loginButton.addActionListener(e -> submit(usernameField, passwordField, false));
        registerButton.addActionListener(e -> submit(usernameField, passwordField, true));

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

    private void submit(JTextField username, JPasswordField password, boolean isRegister) {
        pendingUsername = username.getText().trim();
        pendingPassword = new String(password.getPassword());
        if (isRegister) {
            gateway.register(pendingUsername, pendingPassword);
        } else {
            gateway.login(pendingUsername, pendingPassword);
        }
    }

    private void handleAuthOk(JsonObject json) {
        AuthOkMessage msg = gson.fromJson(json, AuthOkMessage.class);
        SwingUtilities.invokeLater(() -> {
            gateway.unsubscribeAll(this);
            dispose();
            onAuthenticated.accept(msg.username(), pendingPassword);
        });
    }

    private void handleResume(JsonObject json) {
        ResumeMessage msg = gson.fromJson(json, ResumeMessage.class);
        SwingUtilities.invokeLater(() -> {
            gateway.unsubscribeAll(this);
            dispose();
            onResumed.accept(msg.color(), pendingUsername);
        });
    }

    private void handleError(JsonObject json) {
        ErrorMessage err = gson.fromJson(json, ErrorMessage.class);
        SwingUtilities.invokeLater(() -> statusLabel.setText(err.detail()));
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        return l;
    }
}
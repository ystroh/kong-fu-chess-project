package com.chessgame.client.ui;

import com.chessgame.client.network.ServerConnection;
import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.request.CancelRoomMessage;
import com.chessgame.common.protocol.request.CreateRoomMessage;
import com.chessgame.common.protocol.request.JoinRoomMessage;
import com.chessgame.common.protocol.request.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class RoomScreen extends JFrame {
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 15);

    private final ServerConnection connection;
    private final Consumer<Piece.Color> onGameStarted;
    private final Gson gson = new Gson();
    private JLabel statusLabel;

    public RoomScreen(ServerConnection connection, String username, Consumer<Piece.Color> onGameStarted, Runnable onBack) {
        super("Kong Fu Chess - Room");
        this.connection = connection;
        this.onGameStarted = onGameStarted;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 340);
        setLocationRelativeTo(null);

        connection.setMessageListener(this::handleServerMessage);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.BLACK);
        content.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        content.add(buildBody(onBack), BorderLayout.CENTER);
        setContentPane(content);
    }

    private JPanel buildBody(Runnable onBack) {
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        JTextField roomField = new JTextField(15);
        roomField.setFont(LABEL_FONT);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setBackground(Color.BLACK);
        JButton createButton = buildButton("Create");
        JButton joinButton = buildButton("Join");
        JButton cancelButton = buildButton("Cancel");
        buttons.add(createButton);
        buttons.add(joinButton);
        buttons.add(cancelButton);

        createButton.addActionListener(e ->
                connection.send(toJson(MessageType.CREATE_ROOM, new CreateRoomMessage(roomField.getText().trim()))));
        joinButton.addActionListener(e ->
                connection.send(toJson(MessageType.JOIN_ROOM, new JoinRoomMessage(roomField.getText().trim()))));
        cancelButton.addActionListener(e ->
                connection.send(toJson(MessageType.CANCEL_ROOM, new CancelRoomMessage(roomField.getText().trim()))));

        JButton backButton = new JButton("חזרה לתפריט");
        backButton.addActionListener(e -> {
            dispose();
            onBack.run();
        });

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);

        gbc.gridy = 0;
        body.add(label("שם חדר:"), gbc);
        gbc.gridy = 1;
        body.add(roomField, gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(16, 0, 8, 0);
        body.add(buttons, gbc);
        gbc.gridy = 3;
        body.add(statusLabel, gbc);
        gbc.gridy = 4;
        gbc.insets = new Insets(20, 0, 0, 0);
        body.add(backButton, gbc);

        return body;
    }

    private String toJson(MessageType type, Object payload) {
        JsonObject json = gson.toJsonTree(payload).getAsJsonObject();
        json.addProperty("type", type.name());
        return json.toString();
    }

    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("ROLE:")) {
                String value = message.substring("ROLE:".length());
                Piece.Color color = value.equals("SPECTATOR") ? null : Piece.Color.valueOf(value);
                dispose();
                onGameStarted.accept(color);
            } else {
                statusLabel.setText(message);
            }
        });
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(Color.WHITE);
        l.setFont(LABEL_FONT);
        return l;
    }

    private JButton buildButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(ACCENT_GOLD);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(100, 38));
        return button;
    }
}
package com.chessgame.client.ui;

import com.chessgame.client.network.ServerGateway;
import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.response.ErrorMessage;
import com.chessgame.common.protocol.response.RoleMessage;
import com.chessgame.common.protocol.response.RoomCancelledMessage;
import com.chessgame.common.protocol.response.RoomCreatedMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class RoomScreen extends JFrame {
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 15);

    private final ServerGateway gateway;
    private final Consumer<Piece.Color> onGameStarted;
    private final Gson gson = new Gson();
    private JLabel statusLabel;
    private JTextField roomField;

    public RoomScreen(ServerGateway gateway, Consumer<Piece.Color> onGameStarted, Runnable onBack) {
        super("Kong Fu Chess - Room");
        this.gateway = gateway;
        this.onGameStarted = onGameStarted;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 340);
        setLocationRelativeTo(null);

        gateway.subscribe(this, ServerMessageType.ROLE, this::handleRole);
        gateway.subscribe(this, ServerMessageType.ROOM_CREATED, this::handleRoomCreated);
        gateway.subscribe(this, ServerMessageType.ROOM_CANCELLED, this::handleRoomCancelled);
        gateway.subscribe(this, ServerMessageType.ERROR, this::handleError);

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

        roomField = new JTextField(15);
        roomField.setFont(LABEL_FONT);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setBackground(Color.BLACK);
        JButton createButton = buildButton("Create");
        JButton joinButton = buildButton("Join");
        JButton cancelButton = buildButton("Cancel");
        buttons.add(createButton);
        buttons.add(joinButton);
        buttons.add(cancelButton);

        createButton.addActionListener(e -> gateway.createRoom(roomField.getText().trim()));
        joinButton.addActionListener(e -> gateway.joinRoom(roomField.getText().trim()));
        cancelButton.addActionListener(e -> gateway.cancelRoom(roomField.getText().trim()));

        JButton backButton = new JButton("חזרה לתפריט");
        backButton.addActionListener(e -> {
            gateway.unsubscribeAll(this);
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

    private void handleRole(JsonObject json) {
        RoleMessage msg = gson.fromJson(json, RoleMessage.class);
        SwingUtilities.invokeLater(() -> {
            gateway.unsubscribeAll(this);
            dispose();
            onGameStarted.accept(msg.color());
        });
    }

    private void handleRoomCreated(JsonObject json) {
        RoomCreatedMessage msg = gson.fromJson(json, RoomCreatedMessage.class);
        SwingUtilities.invokeLater(() -> statusLabel.setText("חדר נוצר: " + msg.roomName()));
    }

    private void handleRoomCancelled(JsonObject json) {
        RoomCancelledMessage msg = gson.fromJson(json, RoomCancelledMessage.class);
        SwingUtilities.invokeLater(() -> statusLabel.setText("חדר בוטל: " + msg.roomName()));
    }

    private void handleError(JsonObject json) {
        ErrorMessage err = gson.fromJson(json, ErrorMessage.class);
        SwingUtilities.invokeLater(() -> statusLabel.setText(err.detail()));
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
package com.chessgame.client.ui;

import com.chessgame.client.network.ServerConnection;
import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.request.MessageType;
import com.chessgame.common.protocol.request.PlayMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class MenuScreen extends JFrame {
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 16);

    private final ServerConnection connection;
    private final String username;
    private final Consumer<Piece.Color> onGameStarted;
    private final Gson gson = new Gson();
    private JLabel statusLabel;

    public MenuScreen(ServerConnection connection, String username, Consumer<Piece.Color> onGameStarted) {
        super("Kong Fu Chess - Menu");
        this.connection = connection;
        this.username = username;
        this.onGameStarted = onGameStarted;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null);

        connection.setMessageListener(this::handleServerMessage);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(Color.BLACK);
        content.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel welcome = new JLabel("שלום, " + username, SwingConstants.CENTER);
        welcome.setForeground(ACCENT_GOLD);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        content.add(welcome, BorderLayout.NORTH);
        content.add(buildBody(), BorderLayout.CENTER);
        setContentPane(content);
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);

        JButton playButton = buildButton("Play");
        playButton.addActionListener(e -> {
            connection.send(toJson(MessageType.PLAY, new PlayMessage()));
            statusLabel.setText("ממתין ליריב...");
        });

        JButton roomButton = buildButton("Room");
        roomButton.addActionListener(e -> {
            dispose();
            RoomScreen roomScreen = new RoomScreen(connection, username, onGameStarted, () -> {
                MenuScreen back = new MenuScreen(connection, username, onGameStarted);
                back.setVisible(true);
            });
            roomScreen.setVisible(true);
        });

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);

        gbc.gridy = 0;
        body.add(playButton, gbc);
        gbc.gridy = 1;
        body.add(roomButton, gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(20, 0, 0, 0);
        body.add(statusLabel, gbc);

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

    private JButton buildButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(ACCENT_GOLD);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(160, 44));
        return button;
    }
}
package com.chessgame.client.ui;

import com.chessgame.client.network.ServerGateway;
import com.chessgame.common.model.Piece;
import com.chessgame.common.protocol.response.RoleMessage;
import com.chessgame.common.protocol.response.ServerMessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class MenuScreen extends JFrame {
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 16);

    private final ServerGateway gateway;
    private final String username;
    private final Consumer<Piece.Color> onGameStarted;
    private final Gson gson = new Gson();
    private JLabel statusLabel;

    public MenuScreen(ServerGateway gateway, String username, Consumer<Piece.Color> onGameStarted) {
        super("Kong Fu Chess - Menu");
        this.gateway = gateway;
        this.username = username;
        this.onGameStarted = onGameStarted;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null);

        gateway.subscribe(this, ServerMessageType.ROLE, this::handleRole);

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
            gateway.play();
            statusLabel.setText("ממתין ליריב...");
        });

        JButton roomButton = buildButton("Room");
        roomButton.addActionListener(e -> {
            gateway.unsubscribeAll(this);
            dispose();
            RoomScreen roomScreen = new RoomScreen(gateway, onGameStarted, () -> {
                MenuScreen back = new MenuScreen(gateway, username, onGameStarted);
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

    private void handleRole(JsonObject json) {
        RoleMessage msg = gson.fromJson(json, RoleMessage.class);
        SwingUtilities.invokeLater(() -> {
            gateway.unsubscribeAll(this);
            dispose();
            onGameStarted.accept(msg.color());
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
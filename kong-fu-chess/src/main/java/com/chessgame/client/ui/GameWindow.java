package com.chessgame.client.ui;

import com.chessgame.client.ui.board.BoardController;
import com.chessgame.client.ui.moves.PlayerPanelController;
import com.chessgame.common.model.Piece;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public final class GameWindow {

    private static final double SIDE_PANEL_WIDTH_PERCENT = 0.16;
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);

    private final GameStateCoordinator coordinator;
    private final String whitePlayerName;
    private final String blackPlayerName;
    private final Runnable onExit;

    private JFrame frame;
    private BoardController boardController;
    private PlayerPanelController whiteController;
    private PlayerPanelController blackController;

    public GameWindow(GameStateCoordinator coordinator, String whitePlayerName, String blackPlayerName, Runnable onExit) {
        this.coordinator = coordinator;
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
        this.onExit = onExit;
    }

    public void init() {
        coordinator.subscribeToServer();

        boardController = new BoardController(coordinator, whitePlayerName, blackPlayerName);
        whiteController = new PlayerPanelController(coordinator, Piece.Color.WHITE, whitePlayerName);
        blackController = new PlayerPanelController(coordinator, Piece.Color.BLACK, blackPlayerName);

        coordinator.onOpponentStatusChanged(whiteController::showOpponentStatus);

        SoundController soundController = new SoundController(coordinator);

        JPanel contentWrapper = new JPanel(new BorderLayout(16, 16));
        contentWrapper.setBackground(Color.BLACK);
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentWrapper.add(buildTopBar(), BorderLayout.NORTH);
        contentWrapper.add(blackController.panel(), BorderLayout.WEST);
        contentWrapper.add(boardController.panel(), BorderLayout.CENTER);
        contentWrapper.add(whiteController.panel(), BorderLayout.EAST);

        frame = new JFrame("Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(contentWrapper);
        frame.setSize(1100, 800);

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSidePanelWidths();
            }
        });

        frame.setVisible(true);
        updateSidePanelWidths();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.BLACK);

        JButton exitButton = new JButton("יציאה לתפריט");
        exitButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        exitButton.setBackground(ACCENT_GOLD);
        exitButton.setForeground(Color.BLACK);
        exitButton.setFocusPainted(false);
        exitButton.addActionListener(e -> {
            coordinator.gateway().resign();
            coordinator.unsubscribeAll();
            frame.dispose();
            onExit.run();
        });

        bar.add(exitButton);
        return bar;
    }

    private void updateSidePanelWidths() {
        int panelWidth = (int) (frame.getWidth() * SIDE_PANEL_WIDTH_PERCENT);
        whiteController.panel().setPreferredSize(new Dimension(panelWidth, 0));
        blackController.panel().setPreferredSize(new Dimension(panelWidth, 0));
        frame.revalidate();
    }
}

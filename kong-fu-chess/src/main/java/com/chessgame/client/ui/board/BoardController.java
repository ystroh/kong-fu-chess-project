package com.chessgame.client.ui.board;

import com.chessgame.client.input.Controller;
import com.chessgame.client.ui.GameStateCoordinator;
import com.chessgame.client.ui.SnapshotListener;
import com.chessgame.common.engine.GameSnapshot;
import com.chessgame.common.model.Position;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public final class BoardController implements SnapshotListener {

    private static final double MARGIN_PERCENT = 0.06;

    private record BoardGeometry(int boardSize, int offsetX, int offsetY, int cellSize) {
    }

    private final UiMapper uiMapper = new UiMapper();
    private final RenderUI renderUI;
    private final ChessBoardPanel boardPanel = new ChessBoardPanel();

    private volatile Controller controller;
    private BoardGeometry geometry;
    private Integer knownCols;
    private GameSnapshot lastReceivedSnapshot;

    public BoardController(GameStateCoordinator coordinator, String whitePlayerName, String blackPlayerName) {
        this.renderUI = new RenderUI(uiMapper, whitePlayerName, blackPlayerName);

        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });

        boardPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (knownCols != null) {
                    recomputeGeometry(knownCols);
                }
                render(lastReceivedSnapshot);
            }
        });

        coordinator.addListener(this);
        coordinator.onControllerReady(this::setController);
    }

    public ChessBoardPanel panel() {
        return boardPanel;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void onSnapshotUpdated(GameSnapshot snapshot) {
        lastReceivedSnapshot = snapshot;

        if (snapshot != null && (knownCols == null || knownCols != snapshot.width())) {
            knownCols = snapshot.width();
            recomputeGeometry(knownCols);
        }

        render(snapshot);
    }

    private void handleClick(int rawX, int rawY) {
        if (controller == null || geometry == null) {
            return;
        }

        int relX = rawX - geometry.offsetX();
        int relY = rawY - geometry.offsetY();

        boolean outsideBoard = relX < 0 || relY < 0
                || relX >= geometry.boardSize() || relY >= geometry.boardSize();
        if (outsideBoard) {
            return;
        }

        controller.setCellSizePx(geometry.cellSize());
        controller.click(relX, relY);
    }

    private void render(GameSnapshot networkSnapshot) {
        if (networkSnapshot == null || geometry == null) {
            return;
        }
        uiMapper.setCellSize(geometry.cellSize());

        Position selected = (controller != null) ? controller.selectedCell() : null;

        GameSnapshot forRender = new GameSnapshot(
                networkSnapshot.width(), networkSnapshot.height(), networkSnapshot.pieces(),
                selected,
                networkSnapshot.isGameOver(), networkSnapshot.winner(),
                networkSnapshot.moveHistory(), networkSnapshot.scores());

        BufferedImage frameImage = renderUI.renderNewFrame(forRender);
        boardPanel.setBoardImage(frameImage, geometry.offsetX(), geometry.offsetY());
    }

    private void recomputeGeometry(int cols) {
        int panelWidth = boardPanel.getWidth();
        int panelHeight = boardPanel.getHeight();

        int availableSize = Math.min(panelWidth, panelHeight);
        int margin = (int) (availableSize * MARGIN_PERCENT);
        int rawBoardSize = availableSize - (2 * margin);

        int cellSize = Math.max(1, rawBoardSize / cols);
        int boardSize = cellSize * cols;

        int offsetX = (panelWidth - boardSize) / 2;
        int offsetY = (panelHeight - boardSize) / 2;

        geometry = new BoardGeometry(boardSize, offsetX, offsetY, cellSize);
    }
}

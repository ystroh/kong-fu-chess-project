package com.chessgame.server.engine;

import com.chessgame.common.engine.GameSnapshot;
import com.chessgame.common.engine.MoveChannel;
import com.chessgame.common.engine.MoveRecord;
import com.chessgame.common.engine.MoveResult;
import com.chessgame.common.model.Board;
import com.chessgame.common.model.Piece;
import com.chessgame.common.model.Position;
import com.chessgame.common.rules.MoveReason;
import com.chessgame.server.model.GameState;
import com.chessgame.server.realtime.RealTimeArbiter;
import com.chessgame.server.realtime.cooldown.CooldownManager;
import com.chessgame.server.realtime.motion.Motion;
import com.chessgame.server.rules.MoveValidation;
import com.chessgame.server.rules.RuleEngine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class GameEngine implements MoveChannel {
    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter realTimeArbiter;
    private final MoveHistory moveHistory = new MoveHistory();
    private final List<Piece> roster;
    private final List<GameListener> listeners = new ArrayList<>();

    public GameEngine(Board board, GameState gameState, RuleEngine ruleEngine, RealTimeArbiter realTimeArbiter) {
        this.board = board;
        this.gameState = gameState;
        this.ruleEngine = ruleEngine;
        this.realTimeArbiter = realTimeArbiter;
        this.roster = board.allPieces();
    }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (GameListener listener : listeners) {
            listener.onGameStateChanged(this);
        }
    }

    public int score(Piece.Color color) {
        return ScoreCalculator.score(roster, color);
    }

    public MoveResult requestMove(Position source, Position destination) {
        if (gameState.isGameOver()) {
            return MoveResult.rejected(MoveReason.GAME_OVER);
        }

        if (!realTimeArbiter.canStartMotion(source, destination)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }

        MoveValidation legality = ruleEngine.validateMove(source, destination);
        if (!legality.isValid()) {
            return MoveResult.rejected(legality.reason());
        }

        Piece piece = board.pieceAt(source);
        boolean capture = board.pieceAt(destination) != null;
        long timestamp = realTimeArbiter.gameClock();

        realTimeArbiter.startMotion(source, destination);
        moveHistory.record(new MoveRecord(piece.color(), piece.kind(), source, destination, capture, timestamp));
        notifyListeners();

        boolean isOver = gameState.isGameOver();
        return MoveResult.accepted(capture, isOver, isOver ? determineWinner() : null);
    }

    public List<MoveRecord> moveHistory() {
        return moveHistory.all();
    }

    public MoveResult requestJump(Position position) {
        if (gameState.isGameOver()) {
            return MoveResult.rejected(MoveReason.GAME_OVER);
        }

        if (!realTimeArbiter.canStartJump(position)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }

        if (board.pieceAt(position) == null) {
            return MoveResult.rejected(MoveReason.EMPTY_SOURCE);
        }

        realTimeArbiter.startJump(position);

        boolean isOver = gameState.isGameOver();
        // capture=false תמיד: תפיסה-בקפיצה (אם-קורית) נפתרת-רק-מאוחר-יותר, בתוך wait()/advanceTime,
        // כשכלי-אחר מגיע-לאותה-משבצת - לא-ניתן-לדעת-זאת-ברגע-הקפיצה-עצמה (החלטה: אפשרות-א, לא-מדווחים-כרגע).
        return MoveResult.accepted(false, isOver, isOver ? determineWinner() : null);
    }

    public void wait(int milliseconds) {
        boolean kingCaptured = realTimeArbiter.advanceTime(milliseconds);
        if (kingCaptured) {
            gameState.setGameOver(true);
        }
        notifyListeners();
    }

    public GameSnapshot snapshot(Position selectedCell) {
        List<GameSnapshot.PieceView> pieces = collectPieceViews();
        boolean isGameOver = gameState.isGameOver();
        Piece.Color winner = isGameOver ? determineWinner() : null;

        Map<Piece.Color, Integer> scores = new EnumMap<>(Piece.Color.class);
        scores.put(Piece.Color.WHITE, score(Piece.Color.WHITE));
        scores.put(Piece.Color.BLACK, score(Piece.Color.BLACK));

        return new GameSnapshot(board.width(), board.height(), pieces, selectedCell, isGameOver, winner,
                moveHistory(), scores);
    }

    private List<GameSnapshot.PieceView> collectPieceViews() {
        List<GameSnapshot.PieceView> pieces = new ArrayList<>();
        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Piece piece = board.pieceAt(new Position(row, col));
                if (piece != null) {
                    pieces.add(toPieceView(piece));
                }
            }
        }
        return pieces;
    }

    private GameSnapshot.PieceView toPieceView(Piece piece) {
        if (piece.state() == Piece.State.MOVING) {
            return movingPieceView(piece);
        }
        if (piece.state() == Piece.State.COOLDOWN_LONG || piece.state() == Piece.State.COOLDOWN_SHORT) {
            return cooldownPieceView(piece);
        }
        return new GameSnapshot.PieceView(piece.id(), piece.color(), piece.kind(), piece.cell(), piece.state());
    }

    private GameSnapshot.PieceView movingPieceView(Piece piece) {
        Motion motion = realTimeArbiter.motionOf(piece.cell());
        Position destination = (motion != null) ? motion.destination() : null;
        long startTime = (motion != null) ? motion.startTime() : 0;
        long arrivalTime = (motion != null) ? motion.arrivalTime() : 0;

        double[] display = MotionInterpolator.displayPosition(
                piece.cell(), destination, startTime, arrivalTime, realTimeArbiter.gameClock());

        return new GameSnapshot.PieceView(
                piece.id(), piece.color(), piece.kind(), piece.cell(), piece.state(),
                display[0], display[1]);
    }

    private GameSnapshot.PieceView cooldownPieceView(Piece piece) {
        CooldownManager.CooldownWindow window = realTimeArbiter.cooldownOf(piece.cell());
        double remaining = (window != null)
                ? CooldownInterpolator.remainingFraction(window.startTime(), window.endTime(), realTimeArbiter.gameClock())
                : 0.0;

        return new GameSnapshot.PieceView(
                piece.id(), piece.color(), piece.kind(), piece.cell(), piece.state(), remaining);
    }

    private Piece.Color determineWinner() {
        boolean whiteKingAlive = false;
        boolean blackKingAlive = false;

        for (Piece piece : roster) {
            if (piece.kind() != Piece.Kind.KING || piece.state() == Piece.State.CAPTURED) {
                continue;
            }
            if (piece.color() == Piece.Color.WHITE) {
                whiteKingAlive = true;
            } else if (piece.color() == Piece.Color.BLACK) {
                blackKingAlive = true;
            }
        }

        if (whiteKingAlive && !blackKingAlive) return Piece.Color.WHITE;
        if (blackKingAlive && !whiteKingAlive) return Piece.Color.BLACK;
        return null;
    }
}

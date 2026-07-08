import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
class GameEngine {
    private final Board board;
    private final List<ActiveMove> ongoingMoves = new ArrayList<>();
    private final List<AirborneMove> airborneMoves = new ArrayList<>();

    private int selectedRow = -1;
    private int selectedCol = -1;
    private long gameClock = 0;
    private GameState currentState = GameState.INIT;
    private int expectedCols = -1;
    private boolean gameOver = false;
    private static final String VALID_TOKEN_REGEX = "^(\\.|[wb][KQRBNP])$";
    private static final long JUMP_DURATION_MS = 1000L;

    public GameEngine() {
        this.board = new Board();
    }

    public void start() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }

    private void processLine(String line) {
        if (line.isEmpty()) return;

        if (line.equals("Board:")) {
            currentState = GameState.PARSING_BOARD;
            return;
        } else if (line.startsWith("Commands:")) {
            currentState = GameState.PARSING_COMMANDS;
            return;
        }

        if (currentState == GameState.PARSING_BOARD) {
            handleBoardParsing(line);
        } else if (currentState == GameState.PARSING_COMMANDS) {
            handleCommandParsing(line);
        }
    }

    private void handleBoardParsing(String line) {
        String[] tokens = line.split("\\s+");
        for (String token : tokens) {
            if (!token.matches(VALID_TOKEN_REGEX)) {
                System.out.println("ERROR UNKNOWN_TOKEN");
                System.exit(0);
            }
        }
        if (expectedCols == -1) {
            expectedCols = tokens.length;
        } else if (tokens.length != expectedCols) {
            System.out.println("ERROR ROW_WIDTH_MISMATCH");
            System.exit(0);
        }
        board.addRow(tokens);
    }

    private void handleCommandParsing(String line) {
        if (line.startsWith("click")) {
            if (gameOver) return;
            executeClickCommand(line);
        } else if (line.startsWith("wait")) {
            if (gameOver) return;
            executeWaitCommand(line);
        } else if (line.startsWith("jump")) {
            if (gameOver) return;
            executeJumpCommand(line);
        } else if (line.equals("print board")) {
            board.print();
        }
    }

    private boolean isPathBlocked(int fromRow, int fromCol, int toRow, int toCol) {
        int deltaRow = Math.abs(toRow - fromRow);
        int deltaCol = Math.abs(toCol - fromCol);
        boolean isStraightLine = deltaRow == 0 || deltaCol == 0 || deltaRow == deltaCol;
        if (!isStraightLine) return false;

        List<int[]> newPath = getPathCoordinates(fromRow, fromCol, toRow, toCol);

        for (ActiveMove move : ongoingMoves) {
            List<int[]> activePath = getPathCoordinates(move.fromRow, move.fromCol, move.toRow, move.toCol);

            for (int[] p1 : newPath) {
                for (int[] p2 : activePath) {
                    if (p1[0] == p2[0] && p1[1] == p2[1]) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<int[]> getPathCoordinates(int r1, int c1, int r2, int c2) {
        List<int[]> path = new ArrayList<>();
        int dr = Integer.compare(r2, r1);
        int dc = Integer.compare(c2, c1);

        int currR = r1;
        int currC = c1;

        while (currR != r2 || currC != c2) {
            path.add(new int[]{currR, currC});
            currR += dr;
            currC += dc;
        }
        path.add(new int[]{r2, c2});
        return path;
    }

    private boolean isPieceMoving(int row, int col) {
        for (ActiveMove move : ongoingMoves) {
            if (move.fromRow == row && move.fromCol == col) {
                return true;
            }
        }
        return false;
    }

    /**
     * בודק האם הכלי במשבצת מסוימת נמצא כרגע באוויר (מבצע קפיצה).
     */
    private boolean isPieceAirborne(int row, int col) {
        for (AirborneMove jump : airborneMoves) {
            if (jump.row == row && jump.col == col) {
                return true;
            }
        }
        return false;
    }

    private void executeClickCommand(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) return;

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);

        if (x < 0 || y < 0) return;

        int col = x / 100;
        int row = y / 100;

        if (!board.isValidCell(row, col)) return;

        String clickedPiece = board.getPiece(row, col);

        if (selectedRow == -1 && selectedCol == -1) {
            if (!clickedPiece.equals(".")) {
                if (isPieceMoving(row, col) || isPieceAirborne(row, col)) return;
                selectedRow = row;
                selectedCol = col;
            }
        } else {
            if (isPieceMoving(row, col)) return;

            String selectedPiece = board.getPiece(selectedRow, selectedCol);

            if (!clickedPiece.equals(".") && isFriendly(selectedPiece, clickedPiece)) {
                if (isPieceAirborne(row, col)) return;
                selectedRow = row;
                selectedCol = col;
            } else {
                MoveValidator validator = new MoveValidator(board, selectedPiece, selectedRow, selectedCol, row, col);

                if (!validator.isValid()) {
                    selectedRow = -1;
                    selectedCol = -1;
                    return;
                }

                if (isPathBlocked(selectedRow, selectedCol, row, col)) {
                    selectedRow = -1;
                    selectedCol = -1;
                    return;
                }

                if (isSquareReserved(row, col)) {
                    selectedRow = -1;
                    selectedCol = -1;
                    return;
                }

                int distance = Math.max(Math.abs(row - selectedRow), Math.abs(col - selectedCol));
                long moveDuration = distance * 1000L;
                long arrivalTime = gameClock + moveDuration;

                ongoingMoves.add(new ActiveMove(selectedRow, selectedCol, row, col, selectedPiece, arrivalTime));
                selectedRow = -1;
                selectedCol = -1;
            }
        }
    }

    /**
     * מטפל בפקודת jump - כלי קופץ באוויר למשך 1000 מילישניות ונשאר במקומו
     * הלוגי. כלי בתנועה, כלי שכבר באוויר, או משבצת ריקה - לא יכולים לקפוץ.
     */
    private void executeJumpCommand(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) return;

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);

        if (x < 0 || y < 0) return;

        int col = x / 100;
        int row = y / 100;

        if (!board.isValidCell(row, col)) return;

        String piece = board.getPiece(row, col);
        if (piece.equals(".")) return;

        if (isPieceMoving(row, col)) return;
        if (isPieceAirborne(row, col)) return;

        if (selectedRow == row && selectedCol == col) {
            selectedRow = -1;
            selectedCol = -1;
        }

        airborneMoves.add(new AirborneMove(row, col, piece, gameClock + JUMP_DURATION_MS));
    }

    /**
     * מעדכן את שעון המשחק, מבצע מהלכים שהסתיימו, ומטפל בלכידות של כלים
     * מרחפים (jump) שאויב "הגיע" אליהם בזמן שהם עדיין היו באוויר.
     */
    private void executeWaitCommand(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            int ms = Integer.parseInt(parts[1]);
            gameClock += ms;

            Iterator<ActiveMove> iterator = ongoingMoves.iterator();
            while (iterator.hasNext()) {
                ActiveMove move = iterator.next();

                if (gameClock >= move.arrivalTime) {
                    AirborneMove airborneDefender = findAirborneCapturing(move);

                    if (airborneDefender != null) {
                        // הכלי המרחף לוכד את הכלי המגיע - הוא לא נוחת, ומוסר מהלוח.
                        board.clearCell(move.fromRow, move.fromCol);
                        airborneMoves.remove(airborneDefender);
                        iterator.remove();
                        continue;
                    }

                    String capturedPiece = board.getPiece(move.toRow, move.toCol);

                    String pieceToPlace = isPawnPromotion(move) ? move.piece.charAt(0) + "Q" : move.piece;
                    board.setPiece(move.toRow, move.toCol, pieceToPlace);
                    board.clearCell(move.fromRow, move.fromCol);
                    iterator.remove();

                    if (isKing(capturedPiece)) {
                        gameOver = true;
                    }
                }
            }

            Iterator<AirborneMove> jumpIterator = airborneMoves.iterator();
            while (jumpIterator.hasNext()) {
                AirborneMove jump = jumpIterator.next();
                if (gameClock >= jump.landTime) {
                    // הקפיצה הסתיימה בלי שנחת עליה אויב - הכלי פשוט נוחת במקומו.
                    jumpIterator.remove();
                }
            }
        }
    }

    /**
     * מאתר כלי מרחף עוין שנמצא במשבצת היעד של המהלך הנתון, ושהיה עדיין
     * באוויר ברגע הגעת המהלך (jump.landTime >= move.arrivalTime) - כלומר
     * יש ללכוד את הכלי המגיע במקום לתת לו לנחות כרגיל.
     */
    private AirborneMove findAirborneCapturing(ActiveMove move) {
        for (AirborneMove jump : airborneMoves) {
            if (jump.row == move.toRow && jump.col == move.toCol
                    && jump.landTime >= move.arrivalTime
                    && !isFriendly(jump.piece, move.piece)) {
                return jump;
            }
        }
        return null;
    }

    private boolean isPawnPromotion(ActiveMove move) {
        if (move.piece.charAt(1) != 'P') return false;
        int lastRow = (move.piece.charAt(0) == 'w') ? 0 : board.getRowsCount() - 1;
        return move.toRow == lastRow;
    }

    private boolean isKing(String piece) {
        return piece.length() == 2 && piece.charAt(1) == 'K';
    }

    private boolean isSquareReserved(int row, int col) {
        for (ActiveMove move : ongoingMoves) {
            if (move.toRow == row && move.toCol == col) {
                return true;
            }
        }
        return false;
    }

    public boolean isFriendly(String piece1, String piece2) {
        if (piece1.isEmpty() || piece2.isEmpty()) return false;
        return piece1.charAt(0) == piece2.charAt(0);
    }
}
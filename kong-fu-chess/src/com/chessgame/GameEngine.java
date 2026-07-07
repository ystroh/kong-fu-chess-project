package com.chessgame;
class GameEngine {
    private final Board board;
    private final List<ActiveMove> ongoingMoves = new ArrayList<>();

    private int selectedRow = -1;
    private int selectedCol = -1;
    private long gameClock = 0;
    private GameState currentState = GameState.INIT;
    private int expectedCols = -1;

    private static final String VALID_TOKEN_REGEX = "^(\\.|[wb][KQRBNP])$";

    public GameEngine() {
        this.board = new Board();
    }

    /**
     * נקודת ההתחלה של המנוע: קורא קלט מהמשתמש ומפעיל את הפקודות.
     */
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

    /**
     * מנתב את הקלט לפי המצב הנוכחי של המשחק (קליטת לוח או פקודות).
     */
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

    /**
     * מנתח שורה של הלוח ומעדכן את המבנה.
     */
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

    /**
     * מנתב את הפקודות (click, wait, print) לטיפול המתאים.
     */
    private void handleCommandParsing(String line) {
        if (line.startsWith("click")) {
            executeClickCommand(line);
        } else if (line.startsWith("wait")) {
            executeWaitCommand(line);
        } else if (line.equals("print board")) {
            board.print();
        }
    }

    /**
     * בודק האם מסלול התנועה של כלי נחסם על ידי מהלכים אחרים שמתבצעים כעת.
     */
    private boolean isPathBlocked(int fromRow, int fromCol, int toRow, int toCol) {
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

    /**
     * מחזיר רשימת כל המשבצות שהכלי עובר בהן במסלולו.
     */
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

    /**
     * בודק האם הכלי במשבצת מסוימת נמצא כרגע בתנועה.
     */
    private boolean isPieceMoving(int row, int col) {
        for (ActiveMove move : ongoingMoves) {
            if (move.fromRow == row && move.fromCol == col) {
                return true;
            }
        }
        return false;
    }

    /**
     * מטפל בפקודת click - בחירת כלי או ביצוע מהלך.
     */
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
                if (isPieceMoving(row, col)) return;
                selectedRow = row;
                selectedCol = col;
            }
        } else {
            if (isPieceMoving(row, col)) return;
            if (isPathBlocked(selectedRow, selectedCol, row, col)) return;

            String selectedPiece = board.getPiece(selectedRow, selectedCol);

            if (!clickedPiece.equals(".") && isFriendly(selectedPiece, clickedPiece)) {
                if (isPieceMoving(row, col)) return;
                selectedRow = row;
                selectedCol = col;
            } else {
                MoveValidator validator = new MoveValidator(board, selectedPiece, selectedRow, selectedCol, row, col);

                if (!validator.isValid()) {
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
     * מעדכן את שעון המשחק ומבצע מהלכים שהסתיימו בזמן שחלף.
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
                    board.setPiece(move.toRow, move.toCol, move.piece);
                    board.clearCell(move.fromRow, move.fromCol);
                    iterator.remove();
                }
            }
        }
    }

    /**
     * בודק האם המשבצת היעד כבר תפוסה על ידי כלי שנמצא בדרכו אליה.
     */
    private boolean isSquareReserved(int row, int col) {
        for (ActiveMove move : ongoingMoves) {
            if (move.toRow == row && move.toCol == col) {
                return true;
            }
        }
        return false;
    }

    /**
     * בודק האם שני כלים הם מאותו צבע.
     */
    public boolean isFriendly(String piece1, String piece2) {
        if (piece1.isEmpty() || piece2.isEmpty()) return false;
        return piece1.charAt(0) == piece2.charAt(0);
    }
}
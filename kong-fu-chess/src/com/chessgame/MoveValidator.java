package com.chessgame;
/**
 * מחלקה האחראית על אימות חוקיות המהלכים בהתאם לסוג הכלי.
 */
class MoveValidator {
    private final Board board;
    private final char pieceType;
    private final char pieceColor;
    private final int fromRow, fromCol, toRow, toCol;
    private final int deltaRow, deltaCol;

    /**
     * מחשב את נתוני המהלך לבדיקת תקינות.
     */
    public MoveValidator(Board board, String piece, int fromRow, int fromCol, int toRow, int toCol) {
        this.board = board;
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.pieceType = (piece != null && piece.length() >= 2) ? piece.charAt(1) : '?';
        this.pieceColor = (piece != null && piece.length() >= 1) ? piece.charAt(0) : '?';
        this.deltaRow = Math.abs(toRow - fromRow);
        this.deltaCol = Math.abs(toCol - fromCol);
    }

    /**
     * בודק האם המהלך חוקי לפי סוג הכלי שנבחר.
     */
    public boolean isValid() {
        if (fromRow == toRow && fromCol == toCol) return false;
        if (pieceType == '?') return false;

        switch (pieceType) {
            case 'K': return isValidKingMove();
            case 'R': return isValidRookMove();
            case 'B': return isValidBishopMove();
            case 'Q': return isValidQueenMove();
            case 'N': return isValidKnightMove();
            case 'P': return isValidPawnMove();
            default: return false;
        }
    }

    private boolean isValidKingMove() {
        return deltaRow <= 1 && deltaCol <= 1;
    }

    private boolean isValidRookMove() {
        return (deltaRow == 0 || deltaCol == 0) && isPathClear();
    }

    private boolean isValidBishopMove() {
        return (deltaRow == deltaCol) && isPathClear();
    }

    private boolean isValidQueenMove() {
        return (deltaRow == deltaCol || deltaRow == 0 || deltaCol == 0) && isPathClear();
    }

    private boolean isValidKnightMove() {
        return (deltaRow == 1 && deltaCol == 2) || (deltaRow == 2 && deltaCol == 1);
    }

    private boolean isValidPawnMove() {
        int direction = (pieceColor == 'w') ? -1 : 1;

        if (deltaCol == 0 && toRow == fromRow + direction) {
            return board.getPiece(toRow, toCol).equals(".");
        }

        if (deltaCol == 1 && toRow == fromRow + direction) {
            String target = board.getPiece(toRow, toCol);
            return !target.equals(".") && target.charAt(0) != pieceColor;
        }

        return false;
    }

    /**
     * בודק האם המסלול של הכלי פנוי (ללא כלים אחרים בדרך).
     */
    private boolean isPathClear() {
        int stepRow = Integer.compare(toRow, fromRow);
        int stepCol = Integer.compare(toCol, fromCol);

        int currRow = fromRow + stepRow;
        int currCol = fromCol + stepCol;

        while (currRow != toRow || currCol != toCol) {
            if (!board.getPiece(currRow, currCol).equals(".")) {
                return false;
            }
            currRow += stepRow;
            currCol += stepCol;
        }
        return true;
    }
}

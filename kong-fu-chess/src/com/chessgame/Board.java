package com.chessgame;
class Board {
    private final List<String[]> grid;

    public Board() {
        this.grid = new ArrayList<>();
    }

    public void addRow(String[] row) {
        this.grid.add(row);
    }

    public int getRowsCount() {
        return grid.size();
    }

    public int getColsCount() {
        return grid.isEmpty() ? 0 : grid.get(0).length;
    }

    public String getPiece(int row, int col) {
        return grid.get(row)[col];
    }

    public void setPiece(int row, int col, String piece) {
        grid.get(row)[col] = piece;
    }

    public void clearCell(int row, int col) {
        grid.get(row)[col] = ".";
    }

    public boolean isValidCell(int row, int col) {
        return row >= 0 && row < getRowsCount() && col >= 0 && col < getColsCount();
    }

    public void print() {
        for (String[] row : grid) {
            System.out.println(String.join(" ", row));
        }
    }
}
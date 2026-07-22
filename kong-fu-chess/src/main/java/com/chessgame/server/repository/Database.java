package com.chessgame.server.repository;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String URL = "jdbc:sqlite:chessgame.db";

    public Database() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        username TEXT PRIMARY KEY,
                        password_hash TEXT NOT NULL,
                        rating INTEGER NOT NULL DEFAULT 1200
                    )
                    """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
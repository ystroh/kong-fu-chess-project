package com.chessgame.server.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private final String url;
    private final String user;
    private final String password;

    public Database() {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "5432");
        String dbName = System.getenv().getOrDefault("DB_NAME", "chessgame");
        this.user = System.getenv().getOrDefault("DB_USER", "chessuser");
        this.password = System.getenv().getOrDefault("DB_PASSWORD", "chesspass");
        this.url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;

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
        return DriverManager.getConnection(url, user, password);
    }
}
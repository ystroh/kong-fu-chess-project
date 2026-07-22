package com.chessgame.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class UserRepository {

    public record User(String username, String passwordHash, int rating) {
    }

    private final Database database;
    public static final int STARTING_RATING = 1200;
    public UserRepository(Database database) {
        this.database = database;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT username, password_hash, rating FROM users WHERE username = ?";
        try (Connection conn = database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new User(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getInt("rating")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query user", e);
        }
    }

    public void create(String username, String plainPassword) {
        String sql = "INSERT INTO users (username, password_hash, rating) VALUES (?, ?, ?)";
        try (Connection conn = database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, PasswordHasher.hash(plainPassword));
            stmt.setInt(3, STARTING_RATING);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create user", e);
        }
    }

    public void updateRating(String username, int newRating) {
        String sql = "UPDATE users SET rating = ? WHERE username = ?";
        try (Connection conn = database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newRating);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update rating", e);
        }
    }
}
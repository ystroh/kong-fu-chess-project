package com.chessgame.server.bus;

public record MatchAssignment(String gameId, String whiteUsername, String blackUsername) {}
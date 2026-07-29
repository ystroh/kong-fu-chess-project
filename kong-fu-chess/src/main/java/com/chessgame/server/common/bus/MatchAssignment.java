package com.chessgame.server.common.bus;

public record MatchAssignment(String gameId, String whiteUsername, String blackUsername) {}
package com.chessgame.server.bus;

public record MatchmakingRequest(String username, int rating) {}
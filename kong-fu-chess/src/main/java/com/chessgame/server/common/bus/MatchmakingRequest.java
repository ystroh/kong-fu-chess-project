package com.chessgame.server.common.bus;

public record MatchmakingRequest(String username, int rating) {}
package com.chessgame.server.common.bus;

public record GameCreated(String gameId, String whiteUsername, String blackUsername) {}
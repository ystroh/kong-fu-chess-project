package com.chessgame.server.common.bus;

public record RoomCreateRequest(String roomName, String hostUsername) {}
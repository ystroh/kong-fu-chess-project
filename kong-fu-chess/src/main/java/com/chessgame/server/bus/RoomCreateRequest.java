package com.chessgame.server.bus;

public record RoomCreateRequest(String roomName, String hostUsername) {}
package com.chessgame.server.bus;

public record RoomJoinRequest(String roomName, String username) {
}
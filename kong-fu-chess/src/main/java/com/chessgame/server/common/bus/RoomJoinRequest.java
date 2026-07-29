package com.chessgame.server.common.bus;

public record RoomJoinRequest(String roomName, String username) {
}
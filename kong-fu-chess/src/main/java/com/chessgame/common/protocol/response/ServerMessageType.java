package com.chessgame.common.protocol.response;

public enum ServerMessageType {
    AUTH_OK,
    ERROR,
    RESUME,
    ROLE,
    ROOM_CREATED,
    ROOM_CANCELLED,
    GAME_STATE,
    OPPONENT_DISCONNECTED,
    OPPONENT_RECONNECTED
}

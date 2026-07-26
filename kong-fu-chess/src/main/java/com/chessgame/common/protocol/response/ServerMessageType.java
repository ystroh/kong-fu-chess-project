package com.chessgame.common.protocol.response;

public enum ServerMessageType {
    AUTH_OK,
    ERROR,
    RESUME,
    ROLE,
    ROOM_CREATED,
    ROOM_CANCELLED,
    GAME_STATE,
    ACTION_OCCURRED,
    OPPONENT_DISCONNECTED,
    OPPONENT_RECONNECTED
}

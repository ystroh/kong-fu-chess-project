package com.chessgame.server.network;

import com.chessgame.common.protocol.response.ServerMessageType;

public interface ClientGateway {
    void sendTo(String username, ServerMessageType type, Object payload);
}

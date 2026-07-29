package com.chessgame.server.shard;

import com.chessgame.common.protocol.response.ServerMessageType;

public interface ClientGateway {
    void sendTo(String username, ServerMessageType type, Object payload);
}
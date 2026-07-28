package com.chessgame.server.network;

import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.server.bus.NatsEventBus;

public final class NatsClientGateway implements ClientGateway {

    private final NatsEventBus bus;

    public NatsClientGateway(NatsEventBus bus) {
        this.bus = bus;
    }

    @Override
    public void sendTo(String username, ServerMessageType type, Object payload) {
        String json = MessageSerializer.serialize(type, payload);
        bus.publish("client." + username + ".out", json);
    }
}
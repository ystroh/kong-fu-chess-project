package com.chessgame.server.shard;

import com.chessgame.common.protocol.response.ServerMessageType;
import com.chessgame.server.common.bus.NatsEventBus;
import com.chessgame.server.common.bus.NatsSubjects;
import com.chessgame.server.gateway.MessageSerializer;

public final class NatsClientGateway implements ClientGateway {

    private final NatsEventBus bus;

    public NatsClientGateway(NatsEventBus bus) {
        this.bus = bus;
    }

    @Override
    public void sendTo(String username, ServerMessageType type, Object payload) {
        String json = MessageSerializer.serialize(type, payload);
        bus.publish(NatsSubjects.clientOutbox(username), json);
    }
}
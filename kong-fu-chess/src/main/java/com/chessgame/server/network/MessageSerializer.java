package com.chessgame.server.network;

import com.chessgame.common.protocol.response.ServerMessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public final class MessageSerializer {

    private static final Gson gson = new Gson();

    private MessageSerializer() {
    }

    public static String serialize(ServerMessageType type, Object payload) {
        JsonObject json = gson.toJsonTree(payload).getAsJsonObject();
        json.addProperty("type", type.name());
        return json.toString();
    }
}

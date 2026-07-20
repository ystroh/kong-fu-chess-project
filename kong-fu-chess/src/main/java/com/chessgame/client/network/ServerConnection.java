package com.chessgame.client.network;


import java.util.function.Consumer;

public interface ServerConnection {
    void send(String message);
    void setMessageListener(Consumer<String> listener);
}

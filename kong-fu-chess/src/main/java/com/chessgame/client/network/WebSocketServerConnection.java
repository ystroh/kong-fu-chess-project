package com.chessgame.client.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class WebSocketServerConnection implements ServerConnection {

    private final WebSocket socket;
    private volatile Consumer<String> messageListener;

    public WebSocketServerConnection(String serverUrl) {
        HttpClient httpClient = HttpClient.newHttpClient();
        this.socket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(serverUrl), new Listener())
                .join();
    }

    @Override
    public void send(String message) {
        socket.sendText(message, true);
    }

    @Override
    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String fullMessage = buffer.toString();
                buffer.setLength(0);
                if (messageListener != null) {
                    messageListener.accept(fullMessage);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            error.printStackTrace();
        }
    }
}
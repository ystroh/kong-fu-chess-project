package com.chessgame.server.bus;

import com.google.gson.Gson;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

public final class NatsEventBus implements AutoCloseable {

    private final Connection connection;
    private final Gson gson = new Gson();

    public interface Subscription {
        void unsubscribe();
    }

    @FunctionalInterface
    public interface SubjectAwareHandler<T> {
        void handle(String subject, T event);
    }

    public NatsEventBus() {
        this(System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222"));
    }

    public NatsEventBus(String natsUrl) {
        try {
            Options options = new Options.Builder()
                    .server(natsUrl)
                    .connectionTimeout(Duration.ofSeconds(5))
                    .build();
            this.connection = Nats.connect(options);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to NATS at " + natsUrl, e);
        }
    }

    public void publish(String subject, Object payload) {
        String json = gson.toJson(payload);
        connection.publish(subject, json.getBytes(StandardCharsets.UTF_8));
    }

    public <T> Subscription subscribe(String subject, Class<T> type, Consumer<T> handler) {
        Dispatcher dispatcher = connection.createDispatcher(msg -> {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            T event = gson.fromJson(json, type);
            handler.accept(event);
        });
        dispatcher.subscribe(subject);
        return () -> dispatcher.unsubscribe(subject);
    }

    public <T> Subscription subscribeWildcard(String subjectPattern, Class<T> type, SubjectAwareHandler<T> handler) {
        Dispatcher dispatcher = connection.createDispatcher(msg -> {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            T event = gson.fromJson(json, type);
            handler.handle(msg.getSubject(), event);
        });
        dispatcher.subscribe(subjectPattern);
        return () -> dispatcher.unsubscribe(subjectPattern);
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
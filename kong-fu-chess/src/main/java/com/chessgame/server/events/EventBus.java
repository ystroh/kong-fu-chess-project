package com.chessgame.server.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class EventBus {
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add((Consumer<Object>) listener);
    }

    public void publish(Object event) {
        List<Consumer<Object>> forThisType = listeners.get(event.getClass());
        if (forThisType == null) {
            return;
        }
        for (Consumer<Object> listener : forThisType) {
            listener.accept(event);
        }
    }
}
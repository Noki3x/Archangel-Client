package dev.archangelclient.core.event;

import dev.archangelclient.ArchangelClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A tiny synchronous event bus. Listeners are keyed by exact event class; a listener that throws
 * is logged and skipped so one broken feature can't take down the others.
 */
public final class EventBus {
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    /** Registers {@code listener} and returns a handle that removes it again. */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribe(Class<T> type, Consumer<? super T> listener) {
        Consumer<Object> raw = (Consumer<Object>) listener;
        listeners.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>()).add(raw);
        return () -> listeners.getOrDefault(type, List.of()).remove(raw);
    }

    public <T> T post(T event) {
        List<Consumer<Object>> list = listeners.get(event.getClass());
        if (list == null) return event;
        for (Consumer<Object> listener : list) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                ArchangelClient.LOGGER.error("Listener for {} failed", event.getClass().getSimpleName(), e);
            }
        }
        return event;
    }

    @FunctionalInterface
    public interface Subscription {
        void unsubscribe();
    }
}

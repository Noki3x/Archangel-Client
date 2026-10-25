package dev.archangelclient.core.event;

/** Posted at the end of every client tick (20 per second). */
public record TickEvent() {
    public static final TickEvent INSTANCE = new TickEvent();
}

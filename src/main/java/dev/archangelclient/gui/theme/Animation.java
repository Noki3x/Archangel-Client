package dev.archangelclient.gui.theme;

import net.minecraft.util.Util;

/**
 * A value that eases toward a target over a fixed duration, independent of frame rate.
 * Used for hover highlights and fades.
 */
public final class Animation {
    private final long durationMs;
    private float from;
    private float to;
    private long start;

    public Animation(long durationMs, float initial) {
        this.durationMs = durationMs;
        this.from = initial;
        this.to = initial;
    }

    public void target(float value) {
        if (value == to) return;
        from = get();
        to = value;
        start = Util.getMillis();
    }

    public float get() {
        float t = (Util.getMillis() - start) / (float) durationMs;
        return Easing.lerp(from, to, Easing.outCubic(t));
    }
}

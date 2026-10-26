package dev.archangelclient.emotes;

import dev.archangelclient.emotes.anim.LoopMode;

/**
 * One player's running emote. Times are {@code Util.getMillis()}; the pose blends in and out so
 * starting and stopping never snaps.
 */
public final class EmotePlayback {
    static final float BLEND_IN = 0.15f;
    static final float BLEND_OUT = 0.2f;

    final Emote emote;
    final long startMs;
    long stopMs = -1;

    EmotePlayback(Emote emote, long startMs) {
        this.emote = emote;
        this.startMs = startMs;
    }

    public Emote emote() {
        return emote;
    }

    public float elapsed(long now) {
        return (now - startMs) / 1000f;
    }

    public boolean stopping() {
        return stopMs >= 0;
    }

    void stop(long now) {
        if (stopMs < 0) stopMs = now;
    }

    /** True once a one-shot emote has played through, or a stopped emote has blended out. */
    boolean done(long now) {
        if (stopMs >= 0) return (now - stopMs) / 1000f >= BLEND_OUT;
        return emote.animation().finished(elapsed(now));
    }

    /** How much of the emote pose to show, 0..1. */
    public float weight(long now) {
        float elapsed = elapsed(now);
        float w = Math.min(1, elapsed / BLEND_IN);
        var animation = emote.animation();
        if (animation.loop() == LoopMode.ONCE) {
            w = Math.min(w, (animation.length() - elapsed) / BLEND_OUT);
        }
        if (stopMs >= 0) w = Math.min(w, 1 - (now - stopMs) / 1000f / BLEND_OUT);
        return Math.max(0, Math.min(1, w));
    }
}

package dev.archangelclient.emotes.anim;

import java.util.EnumMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** A keyframed animation of the player rig. Rotations are degrees, positions are pixels. */
public record Animation(float length, LoopMode loop, Map<Bone, Track> tracks) {
    public record Track(@Nullable Channel rotation, @Nullable Channel position) {}

    public Animation {
        tracks = new EnumMap<>(tracks);
    }

    /** Animation time (seconds) after {@code elapsed} seconds of playback. */
    public float timeAt(float elapsed) {
        if (loop == LoopMode.LOOP && length > 0) return elapsed % length;
        return Math.min(elapsed, length);
    }

    public boolean finished(float elapsed) {
        return loop == LoopMode.ONCE && elapsed >= length;
    }
}

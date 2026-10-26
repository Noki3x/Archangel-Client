package dev.archangelclient.emotes.anim;

import java.util.List;
import org.joml.Vector3f;

/** A rotation or position channel of one bone: keyframes sorted by time. */
public record Channel(List<Keyframe> keys) {
    public Channel {
        keys = List.copyOf(keys);
    }

    /** Writes the channel's value at {@code time} (seconds) into {@code out}. */
    public Vector3f sample(float time, Vector3f out) {
        int n = keys.size();
        Keyframe first = keys.get(0);
        if (n == 1 || time <= first.time()) return out.set(first.x(), first.y(), first.z());
        Keyframe last = keys.get(n - 1);
        if (time >= last.time()) return out.set(last.x(), last.y(), last.z());

        int i = 1;
        while (keys.get(i).time() < time) i++;
        Keyframe a = keys.get(i - 1), b = keys.get(i);
        float span = b.time() - a.time();
        float t = span <= 0 ? 1 : (time - a.time()) / span;
        if (a.smooth() || b.smooth()) {
            // Catmull-Rom through the neighbouring keys, like Bedrock's "catmullrom" lerp mode
            Keyframe p0 = keys.get(Math.max(0, i - 2)), p3 = keys.get(Math.min(n - 1, i + 1));
            return out.set(catmull(p0.x(), a.x(), b.x(), p3.x(), t),
                    catmull(p0.y(), a.y(), b.y(), p3.y(), t),
                    catmull(p0.z(), a.z(), b.z(), p3.z(), t));
        }
        float e = b.easing().apply(t);
        return out.set(a.x() + (b.x() - a.x()) * e, a.y() + (b.y() - a.y()) * e, a.z() + (b.z() - a.z()) * e);
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5f * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }
}

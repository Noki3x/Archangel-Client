package dev.archangelclient.gui.theme;

/** Easing curves for UI animations. Input and output are in [0, 1]. */
public final class Easing {
    private Easing() {}

    public static float clamp01(float t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    public static float outCubic(float t) {
        float u = 1 - clamp01(t);
        return 1 - u * u * u;
    }

    public static float inOutSine(float t) {
        return (float) (-(Math.cos(Math.PI * clamp01(t)) - 1) / 2);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}

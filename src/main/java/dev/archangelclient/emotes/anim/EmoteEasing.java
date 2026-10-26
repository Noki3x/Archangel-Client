package dev.archangelclient.emotes.anim;

import java.util.Locale;

/**
 * Keyframe easing curves, named as in GeckoLib's keyframe {@code "easing"} field
 * ({@code easeInOutSine}, {@code easeOutBack}, ...). Input and output are progress in [0, 1];
 * back and elastic curves overshoot.
 */
public enum EmoteEasing {
    LINEAR, STEP,
    EASE_IN_SINE, EASE_OUT_SINE, EASE_IN_OUT_SINE,
    EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD,
    EASE_IN_CUBIC, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC,
    EASE_IN_QUART, EASE_OUT_QUART, EASE_IN_OUT_QUART,
    EASE_IN_EXPO, EASE_OUT_EXPO, EASE_IN_OUT_EXPO,
    EASE_IN_BACK, EASE_OUT_BACK, EASE_IN_OUT_BACK,
    EASE_IN_ELASTIC, EASE_OUT_ELASTIC, EASE_IN_OUT_ELASTIC,
    EASE_IN_BOUNCE, EASE_OUT_BOUNCE, EASE_IN_OUT_BOUNCE;

    private static final double BACK = 1.70158, BACK_IO = BACK * 1.525;

    public float apply(float t) {
        double x = t < 0 ? 0 : (t > 1 ? 1 : t);
        return (float) switch (this) {
            case LINEAR -> x;
            case STEP -> x < 1 ? 0 : 1;
            case EASE_IN_SINE -> 1 - Math.cos(x * Math.PI / 2);
            case EASE_OUT_SINE -> Math.sin(x * Math.PI / 2);
            case EASE_IN_OUT_SINE -> -(Math.cos(Math.PI * x) - 1) / 2;
            case EASE_IN_QUAD -> x * x;
            case EASE_OUT_QUAD -> 1 - (1 - x) * (1 - x);
            case EASE_IN_OUT_QUAD -> x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2;
            case EASE_IN_CUBIC -> x * x * x;
            case EASE_OUT_CUBIC -> 1 - Math.pow(1 - x, 3);
            case EASE_IN_OUT_CUBIC -> x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
            case EASE_IN_QUART -> x * x * x * x;
            case EASE_OUT_QUART -> 1 - Math.pow(1 - x, 4);
            case EASE_IN_OUT_QUART -> x < 0.5 ? 8 * Math.pow(x, 4) : 1 - Math.pow(-2 * x + 2, 4) / 2;
            case EASE_IN_EXPO -> x == 0 ? 0 : Math.pow(2, 10 * x - 10);
            case EASE_OUT_EXPO -> x == 1 ? 1 : 1 - Math.pow(2, -10 * x);
            case EASE_IN_OUT_EXPO -> x == 0 || x == 1 ? x
                    : x < 0.5 ? Math.pow(2, 20 * x - 10) / 2 : (2 - Math.pow(2, -20 * x + 10)) / 2;
            case EASE_IN_BACK -> (BACK + 1) * x * x * x - BACK * x * x;
            case EASE_OUT_BACK -> 1 + (BACK + 1) * Math.pow(x - 1, 3) + BACK * Math.pow(x - 1, 2);
            case EASE_IN_OUT_BACK -> x < 0.5
                    ? (Math.pow(2 * x, 2) * ((BACK_IO + 1) * 2 * x - BACK_IO)) / 2
                    : (Math.pow(2 * x - 2, 2) * ((BACK_IO + 1) * (x * 2 - 2) + BACK_IO) + 2) / 2;
            case EASE_IN_ELASTIC -> x == 0 || x == 1 ? x
                    : -Math.pow(2, 10 * x - 10) * Math.sin((x * 10 - 10.75) * (2 * Math.PI / 3));
            case EASE_OUT_ELASTIC -> x == 0 || x == 1 ? x
                    : Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * (2 * Math.PI / 3)) + 1;
            case EASE_IN_OUT_ELASTIC -> x == 0 || x == 1 ? x : x < 0.5
                    ? -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * (2 * Math.PI / 4.5))) / 2
                    : (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * (2 * Math.PI / 4.5))) / 2 + 1;
            case EASE_IN_BOUNCE -> 1 - outBounce(1 - x);
            case EASE_OUT_BOUNCE -> outBounce(x);
            case EASE_IN_OUT_BOUNCE -> x < 0.5 ? (1 - outBounce(1 - 2 * x)) / 2 : (1 + outBounce(2 * x - 1)) / 2;
        };
    }

    private static double outBounce(double x) {
        double n = 7.5625, d = 2.75;
        if (x < 1 / d) return n * x * x;
        if (x < 2 / d) return n * (x -= 1.5 / d) * x + 0.75;
        if (x < 2.5 / d) return n * (x -= 2.25 / d) * x + 0.9375;
        return n * (x -= 2.625 / d) * x + 0.984375;
    }

    /** Parses {@code easeInOutSine}, {@code ease_in_out_sine} or {@code linear}; unknown names fall back to linear. */
    public static EmoteEasing byName(String name) {
        String key = name.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        try {
            return valueOf(key);
        } catch (IllegalArgumentException e) {
            return LINEAR;
        }
    }
}

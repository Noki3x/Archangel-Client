package dev.archangelclient.gui.render;

/** ARGB color helpers. */
public final class Colors {
    private Colors() {}

    public static int alpha(int argb) {
        return argb >>> 24;
    }

    /** Replaces the alpha channel. */
    public static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }

    /** Multiplies the existing alpha by {@code factor} (used for fades). */
    public static int fade(int argb, float factor) {
        return withAlpha(argb, Math.round(alpha(argb) * factor));
    }

    public static int lerp(int a, int b, float t) {
        int r = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            r |= Math.round(ca + (cb - ca) * t) << shift;
        }
        return r;
    }
}

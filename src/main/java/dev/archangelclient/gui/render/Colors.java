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

    /** HSB (each 0..1) to opaque RGB. */
    public static int hsbToRgb(float hue, float saturation, float brightness) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1 - saturation);
        float q = brightness * (1 - saturation * f);
        float t = brightness * (1 - saturation * (1 - f));
        float r, g, b;
        switch ((int) h) {
            case 0 -> { r = brightness; g = t; b = p; }
            case 1 -> { r = q; g = brightness; b = p; }
            case 2 -> { r = p; g = brightness; b = t; }
            case 3 -> { r = p; g = q; b = brightness; }
            case 4 -> { r = t; g = p; b = brightness; }
            default -> { r = brightness; g = p; b = q; }
        }
        return 0xFF000000 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }

    /** RGB to {hue, saturation, brightness}, each 0..1. Alpha is ignored. */
    public static float[] rgbToHsb(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float hue = 0;
        if (d > 0) {
            if (max == r) hue = ((g - b) / d) % 6;
            else if (max == g) hue = (b - r) / d + 2;
            else hue = (r - g) / d + 4;
            hue /= 6;
            if (hue < 0) hue += 1;
        }
        return new float[] {hue, max == 0 ? 0 : d / max, max};
    }
}

package dev.archangelclient.gui.render;

import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 2D drawing primitives built on {@link GuiGraphics#fill}, so they work with the vanilla GUI
 * render pipeline without custom shaders.
 */
public final class RenderUtil {
    private RenderUtil() {}

    /** Filled rectangle with rounded corners, drawn as one horizontal strip per pixel row of the corners. */
    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            // horizontal inset of the corner arc at this row
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    /** Soft glow around a rounded rect: stacked, growing, faint rounded rects. */
    public static void glow(GuiGraphics g, int x, int y, int w, int h, int radius, int color, float strength) {
        for (int i = Theme.GLOW_LAYERS; i >= 1; i--) {
            int a = Math.round(Theme.GLOW_ALPHA * strength * (Theme.GLOW_LAYERS - i + 1) / Theme.GLOW_LAYERS);
            roundedRect(g, x - i, y - i, w + 2 * i, h + 2 * i, radius + i, Colors.withAlpha(color, a));
        }
    }

    /** Round glow fading out from the center: stacked faint discs, smallest (brightest) on top. */
    public static void radialGlow(GuiGraphics g, int cx, int cy, int radius, int color, float strength) {
        for (int i = Theme.RADIAL_GLOW_STEPS; i >= 1; i--) {
            int r = Math.max(1, radius * i / Theme.RADIAL_GLOW_STEPS);
            int a = Math.round(Theme.RADIAL_GLOW_ALPHA * strength);
            if (a > 0) roundedRect(g, cx - r, cy - r, r * 2, r * 2, r, Colors.withAlpha(color, a));
        }
    }

    /** Vertical gradient. */
    public static void gradientV(GuiGraphics g, int x1, int y1, int x2, int y2, int top, int bottom) {
        g.fillGradient(x1, y1, x2, y2, top, bottom);
    }

    /** Horizontal gradient, approximated with 2 px columns. */
    public static void gradientH(GuiGraphics g, int x1, int y1, int x2, int y2, int left, int right) {
        int w = Math.max(1, x2 - x1);
        for (int x = x1; x < x2; x += 2) {
            g.fill(x, y1, Math.min(x + 2, x2), y2, Colors.lerp(left, right, (x - x1) / (float) w));
        }
    }

    /** Darkened edges. {@code strength} is the alpha (0..1) at the very edge. */
    public static void vignette(GuiGraphics g, int w, int h, int color, float strength) {
        int edge = Colors.withAlpha(color, Math.round(255 * strength));
        int clear = Colors.withAlpha(color, 0);
        int bandY = h / 3, bandX = w / 4;
        gradientV(g, 0, 0, w, bandY, edge, clear);
        gradientV(g, 0, h - bandY, w, h, clear, edge);
        gradientH(g, 0, 0, bandX, h, edge, clear);
        gradientH(g, w - bandX, 0, w, h, clear, edge);
    }

    /** Thin horizontal line with a centered glow, used for the username underline. */
    public static void underline(GuiGraphics g, int cx, int y, int width, int color) {
        int half = width / 2;
        glow(g, cx - half, y, width, 1, 1, color, 0.6f);
        g.fill(cx - half, y, cx + half, y + 1, color);
    }
}

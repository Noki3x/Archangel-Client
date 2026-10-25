package dev.archangelclient.gui.render;

import dev.archangelclient.gui.theme.Easing;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The animated backdrop shared by Archangel menus: a charcoal gradient, slow drifting light beams
 * in the accent colors, and a vignette.
 */
public final class MenuBackground {
    private MenuBackground() {}

    public static void render(GuiGraphics g, int width, int height, float alpha) {
        RenderUtil.gradientV(g, 0, 0, width, height, Colors.fade(Theme.BG_LIGHT, alpha), Colors.fade(Theme.BG_DARK, alpha));
        renderBeams(g, width, height, alpha);
        RenderUtil.vignette(g, width, height, Theme.VIGNETTE, Theme.VIGNETTE_STRENGTH * alpha);
    }

    /**
     * Soft light rays fanning down from above the top-right corner (as in the reference). Each ray
     * is a few stacked segments that widen and fade with distance, and sways slowly over time.
     */
    private static void renderBeams(GuiGraphics g, int width, int height, float alpha) {
        float time = (Util.getMillis() % Theme.BEAM_CYCLE_MS) / (float) Theme.BEAM_CYCLE_MS;
        float originX = width * Theme.BEAM_ORIGIN_X;
        float originY = -height * 0.15f;
        int length = (int) (Math.hypot(width, height) * 1.1);
        int segment = length / Theme.BEAM_SEGMENTS;
        for (int i = 0; i < Theme.BEAM_COUNT; i++) {
            float spread = i / (float) (Theme.BEAM_COUNT - 1) - 0.5f;
            float sway = (float) Math.sin((time + i * 0.37f) * Math.PI * 2) * Theme.BEAM_SWAY_RAD;
            float pulse = 0.6f + 0.4f * (float) Math.sin((time * 2 + i * 0.21f) * Math.PI * 2);
            float angle = Theme.BEAM_ANGLE_RAD + spread * Theme.BEAM_FAN_RAD + sway;
            int color = i % 3 == 1 ? Theme.BEAM_TINT : Theme.BEAM_WHITE;
            int baseW = width / 30 + (i % 3) * width / 60;

            g.pose().pushMatrix();
            g.pose().translate(originX, originY);
            g.pose().rotate(angle);
            for (int s = 0; s < Theme.BEAM_SEGMENTS; s++) {
                float f = s / (float) Theme.BEAM_SEGMENTS;
                int w = (int) (baseW * (1f + f * 2.5f));
                int a = Math.round(Theme.BEAM_ALPHA * pulse * (1f - f) * alpha);
                if (a <= 0) continue;
                int y0 = s * segment;
                int y1 = y0 + segment;
                RenderUtil.gradientH(g, -w, y0, 0, y1, Colors.withAlpha(color, 0), Colors.withAlpha(color, a));
                RenderUtil.gradientH(g, 0, y0, w, y1, Colors.withAlpha(color, a), Colors.withAlpha(color, 0));
            }
            g.pose().popMatrix();
        }
    }
}

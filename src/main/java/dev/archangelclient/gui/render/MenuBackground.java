package dev.archangelclient.gui.render;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.gui.theme.MenuStyleModule;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The animated backdrop shared by Archangel menus: a charcoal gradient, slow drifting light beams
 * in the accent colors, and a vignette.
 */
public final class MenuBackground {
    private MenuBackground() {}

    public static void render(GuiGraphics g, int width, int height, float alpha) {
        MenuStyleModule style = ArchangelClient.isReady() ? ArchangelClient.modules().get(MenuStyleModule.class) : null;
        boolean styled = style == null || style.isEnabled();
        RenderUtil.gradientV(g, 0, 0, width, height, Colors.fade(Theme.BG_LIGHT, alpha), Colors.fade(Theme.BG_DARK, alpha));
        if (styled && (style == null || style.isRays())) {
            renderBeams(g, width, height, alpha, style == null ? 1f : style.raySpeed.getFloat(),
                    style == null ? Theme.BEAM_TINT : style.rayTint.get());
        }
        if (styled && (style == null || style.vignette.get())) {
            RenderUtil.vignette(g, width, height, Theme.VIGNETTE, Theme.VIGNETTE_STRENGTH * alpha);
        }
    }

    /**
     * Soft light rays fanning down from above the top-right corner (as in the reference). Each ray
     * is one quad of the generated beam texture, which widens and fades with distance, and sways
     * slowly over time.
     */
    private static void renderBeams(GuiGraphics g, int width, int height, float alpha, float speed, int tint) {
        long cycle = (long) (Theme.BEAM_CYCLE_MS / speed);
        float time = (Util.getMillis() % cycle) / (float) cycle;
        float originX = width * Theme.BEAM_ORIGIN_X;
        float originY = -height * 0.15f;
        int length = (int) (Math.hypot(width, height) * 1.1);
        Identifier beam = WheelTextures.beam();
        for (int i = 0; i < Theme.BEAM_COUNT; i++) {
            float spread = i / (float) (Theme.BEAM_COUNT - 1) - 0.5f;
            float sway = (float) Math.sin((time + i * 0.37f) * Math.PI * 2) * Theme.BEAM_SWAY_RAD;
            float pulse = 0.6f + 0.4f * (float) Math.sin((time * 2 + i * 0.21f) * Math.PI * 2);
            float angle = Theme.BEAM_ANGLE_RAD + spread * Theme.BEAM_FAN_RAD + sway;
            int color = i % 3 == 1 ? tint : Theme.BEAM_WHITE;
            int baseW = width / 30 + (i % 3) * width / 60;
            int farW = Math.round(baseW / WheelTextures.BEAM_START_WIDTH);
            int a = Math.round(Theme.BEAM_ALPHA * pulse * alpha);
            if (a <= 0) continue;

            g.pose().pushMatrix();
            g.pose().translate(originX, originY);
            g.pose().rotate(angle);
            g.blit(RenderPipelines.GUI_TEXTURED, beam, -farW, 0, 0, 0, farW * 2, length, 1, 1, 1, 1, Colors.withAlpha(color, a));
            g.pose().popMatrix();
        }
    }
}

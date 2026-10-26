package dev.archangelclient.gui.screen;

import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Easing;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base for Archangel screens: fades in when opened and, through {@link #fadeOutThen}, fades out
 * before leaving. Subclasses draw with {@link #fade()}, which runs 0 to 1 on open and back to 0 on
 * close, so one value drives both directions.
 */
public abstract class FadingScreen extends Screen {
    private long openedAt;
    private long closingAt;
    private Runnable afterClose;

    protected FadingScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        if (openedAt == 0) openedAt = Util.getMillis();
    }

    /** 0..1 opacity for the open and close transitions. */
    protected float fade() {
        float in = Easing.outCubic((Util.getMillis() - openedAt) / (float) Theme.SCREEN_FADE_MS);
        if (closingAt == 0) return in;
        float out = 1 - Easing.outCubic((Util.getMillis() - closingAt) / (float) Theme.SCREEN_FADE_OUT_MS);
        return Math.min(in, out);
    }

    /**
     * Covers a panel's contents with the panel color while the screen fades, so the contents fade
     * in and out even though GUI drawing has no global opacity. Call last, inside the panel's pose.
     */
    protected void veil(GuiGraphics g, int x, int y, int w, int h) {
        float a = 1 - fade();
        if (a <= 0.01f) return;
        g.nextStratum(); // draw over everything already queued, whatever its pipeline
        RenderUtil.roundedRect(g, x, y, w, h, Theme.RADIUS_LARGE, Colors.fade(Theme.PANEL_SOLID, a));
    }

    protected boolean isClosing() {
        return closingAt != 0;
    }

    /** Plays the fade-out, then runs {@code action} (usually switching screens). Repeated calls are ignored. */
    protected void fadeOutThen(Runnable action) {
        if (closingAt != 0) return;
        closingAt = Util.getMillis();
        afterClose = action;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (afterClose != null && Util.getMillis() - closingAt >= Theme.SCREEN_FADE_OUT_MS) {
            Runnable action = afterClose;
            afterClose = null;
            // after this frame, not in the middle of drawing it
            Minecraft.getInstance().execute(action);
        }
    }
}

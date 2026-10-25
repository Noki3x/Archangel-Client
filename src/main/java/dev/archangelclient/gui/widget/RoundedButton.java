package dev.archangelclient.gui.widget;

import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A minimal text button: plain centered text, and a translucent rounded highlight bar that eases
 * in on hover (as on the reference main menu).
 */
public class RoundedButton extends Button {
    private final Animation hover = new Animation(Theme.HOVER_MS, 0f);
    private float alpha = 1f;

    public RoundedButton(int x, int y, int width, int height, Component label, OnPress onPress) {
        super(x, y, width, height, Fonts.regular(label), onPress, DEFAULT_NARRATION);
    }

    /** Overall opacity, driven by the parent screen's fade-in. */
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hover.target(isHoveredOrFocused() ? 1f : 0f);
        float h = hover.get();
        if (h > 0.01f) {
            RenderUtil.roundedRect(g, getX(), getY(), getWidth(), getHeight(), Theme.RADIUS,
                    Colors.fade(Theme.HOVER_BAR, h * alpha));
        }
        int color = Colors.fade(Colors.lerp(Theme.TEXT_MUTED, Theme.TEXT, h), alpha);
        var font = Minecraft.getInstance().font;
        g.drawCenteredString(font, getMessage(), getX() + getWidth() / 2,
                getY() + (getHeight() - font.lineHeight) / 2 + 1, color);
    }
}

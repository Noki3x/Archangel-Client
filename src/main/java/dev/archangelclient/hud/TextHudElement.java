package dev.archangelclient.hud;

import dev.archangelclient.gui.theme.Fonts;
import net.minecraft.client.gui.GuiGraphics;

/** A HUD element that shows one line of text in a rounded pill. */
public abstract class TextHudElement extends HudElement {
    protected TextHudElement(String name, String description, float defaultX, float defaultY, boolean enabledByDefault) {
        super(name, description, defaultX, defaultY, enabledByDefault);
    }

    protected abstract String text(boolean editor);

    @Override
    public void render(GuiGraphics g, float partialTick, boolean editor) {
        String text = text(editor);
        width = Fonts.width(text) + 10;
        height = LINE_HEIGHT;
        panel(g, width, height);
        text(g, text, 5, 4);
    }
}

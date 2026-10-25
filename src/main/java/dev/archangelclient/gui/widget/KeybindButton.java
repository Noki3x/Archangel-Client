package dev.archangelclient.gui.widget;

import dev.archangelclient.core.setting.KeybindSetting;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * Key capture for a {@link KeybindSetting}: click, then press a key. Escape, Backspace or Delete
 * unbinds. Only one button listens at a time.
 */
public class KeybindButton extends SettingWidget<KeybindSetting> {
    private static final int BOX_WIDTH = 80;
    private static final int BOX_HEIGHT = 16;
    private static KeybindButton listening;

    public KeybindButton(KeybindSetting setting) {
        super(setting);
    }

    /** True while some keybind button is waiting for a key, so the screen shouldn't act on keys. */
    public static boolean isAnyListening() {
        return listening != null;
    }

    public static void stopListening() {
        listening = null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderLabel(g, BOX_WIDTH);
        int bx = x + width - BOX_WIDTH, by = controlY(BOX_HEIGHT);
        boolean active = listening == this;
        boolean hovered = mouseX >= bx && mouseX < bx + BOX_WIDTH && mouseY >= by && mouseY < by + BOX_HEIGHT;
        if (active) RenderUtil.glow(g, bx, by, BOX_WIDTH, BOX_HEIGHT, Theme.RADIUS, Theme.ACCENT, 0.8f);
        RenderUtil.roundedRect(g, bx, by, BOX_WIDTH, BOX_HEIGHT, Theme.RADIUS,
                active ? Colors.withAlpha(Theme.ACCENT, 0x40) : (hovered ? Theme.CARD_HOVER : Theme.FIELD));
        String label = active ? "Press a key…" : setting.keyName().getString();
        Fonts.drawCentered(g, Fonts.trim(label, BOX_WIDTH - 8), bx + BOX_WIDTH / 2, by + 4,
                active ? Theme.ACCENT_LIGHT : (setting.isBound() ? Theme.TEXT : Theme.TEXT_MUTED));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) {
            if (listening == this) listening = null;
            return false;
        }
        listening = listening == this ? null : this;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (listening != this) return false;
        boolean clear = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE;
        setting.set(clear ? KeybindSetting.NONE : key);
        listening = null;
        return true;
    }
}

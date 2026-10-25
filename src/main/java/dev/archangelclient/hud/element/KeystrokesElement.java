package dev.archangelclient.hud.element;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.hud.HudElement;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;

/** Movement keys, mouse buttons and space, lighting up as they're pressed. */
public class KeystrokesElement extends HudElement {
    private static final int KEY = 22;
    private static final int GAP = 2;
    private static final long PRESS_MS = 90;

    private final BooleanSetting showMouse = add(new BooleanSetting("Mouse buttons", "Show left and right mouse buttons", true));
    private final BooleanSetting showCps = add(new BooleanSetting("CPS on buttons", "Show clicks per second on the mouse buttons", true)
            .visibleWhen(showMouse::get));
    private final BooleanSetting showSpace = add(new BooleanSetting("Space bar", "Show the jump key", true));
    private final Animation[] presses = new Animation[7];

    public KeystrokesElement() {
        super("Keystrokes", "Shows WASD, mouse buttons and space as you press them", 0f, 0.42f, true);
        for (int i = 0; i < presses.length; i++) presses[i] = new Animation(PRESS_MS, 0f);
    }

    @Override
    public void render(GuiGraphics g, float partialTick, boolean editor) {
        var o = mc().options;
        width = 3 * KEY + 2 * GAP;
        key(g, 0, o.keyUp, KEY + GAP, 0, KEY, KEY, null);
        key(g, 1, o.keyLeft, 0, KEY + GAP, KEY, KEY, null);
        key(g, 2, o.keyDown, KEY + GAP, KEY + GAP, KEY, KEY, null);
        key(g, 3, o.keyRight, 2 * (KEY + GAP), KEY + GAP, KEY, KEY, null);
        int y = 2 * (KEY + GAP);
        if (showMouse.get()) {
            int half = (width - GAP) / 2;
            var clicks = ArchangelClient.clicks();
            key(g, 4, o.keyAttack, 0, y, half, KEY, showCps.get() ? clicks.left() + " CPS" : null);
            key(g, 5, o.keyUse, half + GAP, y, width - half - GAP, KEY, showCps.get() ? clicks.right() + " CPS" : null);
            y += KEY + GAP;
        }
        if (showSpace.get()) {
            key(g, 6, o.keyJump, 0, y, width, 12, null);
            y += 12 + GAP;
        }
        height = y - GAP;
    }

    private void key(GuiGraphics g, int index, KeyMapping mapping, int x, int y, int w, int h, String subtitle) {
        presses[index].target(mapping.isDown() ? 1f : 0f);
        float p = presses[index].get();
        int bg = background.get() ? Colors.lerp(Theme.PANEL, 0xD0FFFFFF, p) : Colors.withAlpha(0xFFFFFF, Math.round(0xD0 * p));
        RenderUtil.roundedRect(g, x, y, w, h, Theme.RADIUS, bg);
        int color = Colors.lerp(textColor.get(), 0xFF101114, p);
        if (index == 6) {
            // the space bar is a short line rather than a label
            g.fill(x + w / 2 - 12, y + h / 2, x + w / 2 + 12, y + h / 2 + 1, color);
            return;
        }
        String label = index == 4 ? "LMB" : index == 5 ? "RMB" : mapping.getTranslatedKeyMessage().getString();
        label = Fonts.trim(label, w - 4);
        if (subtitle == null) {
            Fonts.drawCentered(g, label, x + w / 2, y + (h - 8) / 2, color);
        } else {
            Fonts.drawCentered(g, label, x + w / 2, y + 3, color);
            g.pose().pushMatrix();
            g.pose().translate(x + w / 2f, y + 13);
            g.pose().scale(0.7f, 0.7f);
            Fonts.drawCentered(g, subtitle, 0, 0, Colors.withAlpha(color, 0xC0));
            g.pose().popMatrix();
        }
    }
}

package dev.archangelclient.gui.widget;

import dev.archangelclient.core.setting.NumberSetting;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Draggable slider for a {@link NumberSetting}, with the current value shown beside it. */
public class Slider extends SettingWidget<NumberSetting> {
    private static final int TRACK_WIDTH = 100;
    private static final int KNOB = 8;
    private final Animation hover = new Animation(Theme.HOVER_MS, 0f);
    private boolean dragging;

    public Slider(NumberSetting setting) {
        super(setting);
    }

    private int trackX() {
        return x + width - TRACK_WIDTH - KNOB / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        String value = setting.format();
        int valueW = Fonts.width(value);
        renderLabel(g, TRACK_WIDTH + valueW + 14);

        int tx = trackX(), ty = controlY(4);
        hover.target(dragging || isOverTrack(mouseX, mouseY) ? 1f : 0f);
        int filled = (int) Math.round(TRACK_WIDTH * setting.getFraction());
        RenderUtil.roundedRect(g, tx, ty, TRACK_WIDTH, 4, 2, Theme.TOGGLE_OFF);
        if (filled > 0) {
            RenderUtil.gradientH(g, tx, ty, tx + filled, ty + 4, Theme.ACCENT, Theme.ACCENT_LIGHT);
        }
        int kx = tx + filled - KNOB / 2, ky = controlY(KNOB);
        RenderUtil.glow(g, kx, ky, KNOB, KNOB, KNOB / 2, Theme.ACCENT_LIGHT, 0.4f + hover.get() * 0.6f);
        RenderUtil.roundedRect(g, kx, ky, KNOB, KNOB, KNOB / 2, 0xFFFFFFFF);
        Fonts.draw(g, value, tx - valueW - 8, controlY(8), Theme.TEXT_MUTED);
    }

    private boolean isOverTrack(double mouseX, double mouseY) {
        return mouseX >= trackX() - KNOB && mouseX <= trackX() + TRACK_WIDTH + KNOB
                && mouseY >= y && mouseY < y + Theme.ROW_HEIGHT;
    }

    private void updateFromMouse(double mouseX) {
        setting.setFraction(Math.max(0, Math.min(1, (mouseX - trackX()) / TRACK_WIDTH)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isOverTrack(mouseX, mouseY)) return false;
        dragging = true;
        updateFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging) return false;
        updateFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = dragging;
        dragging = false;
        return was;
    }
}

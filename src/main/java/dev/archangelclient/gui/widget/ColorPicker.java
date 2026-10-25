package dev.archangelclient.gui.widget;

import dev.archangelclient.core.setting.ColorSetting;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Color picker for a {@link ColorSetting}: a swatch that expands into hue, saturation, brightness
 * (and alpha) bars. Works in HSB internally so the hue survives dragging saturation to zero.
 */
public class ColorPicker extends SettingWidget<ColorSetting> {
    private static final int SWATCH_W = 26;
    private static final int SWATCH_H = 14;
    private static final int BAR_H = 8;
    private static final int BAR_GAP = 7;
    private static final String[] LABELS = {"Hue", "Saturation", "Brightness", "Opacity"};

    private final Animation open = new Animation(Theme.HOVER_MS, 0f);
    private boolean expanded;
    private int dragBar = -1;
    private float hue, sat, bri;
    private int lastSynced;

    public ColorPicker(ColorSetting setting) {
        super(setting);
        sync();
    }

    /** Re-reads HSB from the setting if it changed elsewhere (reset, profile switch). */
    private void sync() {
        int argb = setting.get();
        if (argb == lastSynced && lastSynced != 0) return;
        float[] hsb = Colors.rgbToHsb(argb);
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        lastSynced = argb;
    }

    private int bars() {
        return setting.allowsAlpha() ? 4 : 3;
    }

    private int panelHeight() {
        return bars() * (BAR_H + BAR_GAP + 10) + 6;
    }

    @Override
    public int getHeight() {
        return Theme.ROW_HEIGHT + Math.round(panelHeight() * open.get());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        sync();
        open.target(expanded ? 1f : 0f);
        String hex = String.format(setting.allowsAlpha() ? "#%08X" : "#%06X",
                setting.allowsAlpha() ? setting.get() : setting.get() & 0xFFFFFF);
        int hexW = Fonts.width(hex);
        renderLabel(g, SWATCH_W + hexW + 10);
        int sx = x + width - SWATCH_W, sy = controlY(SWATCH_H);
        Fonts.draw(g, hex, sx - hexW - 8, controlY(8), Theme.TEXT_MUTED);
        RenderUtil.roundedRect(g, sx - 1, sy - 1, SWATCH_W + 2, SWATCH_H + 2, Theme.RADIUS, expanded ? Theme.ACCENT_LIGHT : Theme.TOGGLE_OFF);
        RenderUtil.roundedRect(g, sx, sy, SWATCH_W, SWATCH_H, Theme.RADIUS - 1, setting.get());

        float o = open.get();
        if (o < 0.02f) return;
        int top = y + Theme.ROW_HEIGHT;
        g.enableScissor(x, top, x + width, top + Math.round(panelHeight() * o));
        for (int i = 0; i < bars(); i++) {
            int ly = barY(i) - 11;
            Fonts.draw(g, LABELS[i], x, ly, Theme.TEXT_MUTED);
            renderBar(g, i, barX(), barY(i), barWidth());
        }
        g.disableScissor();
    }

    private int barX() {
        return x;
    }

    private int barWidth() {
        return width;
    }

    private int barY(int i) {
        return y + Theme.ROW_HEIGHT + 12 + i * (BAR_H + BAR_GAP + 10);
    }

    private void renderBar(GuiGraphics g, int bar, int bx, int by, int bw) {
        float value;
        switch (bar) {
            case 0 -> {
                // hue spectrum in six segments
                int seg = bw / 6;
                for (int s = 0; s < 6; s++) {
                    int x1 = bx + s * seg, x2 = s == 5 ? bx + bw : x1 + seg;
                    RenderUtil.gradientH(g, x1, by, x2, by + BAR_H, Colors.hsbToRgb(s / 6f, 1, 1), Colors.hsbToRgb((s + 1) / 6f, 1, 1));
                }
                value = hue;
            }
            case 1 -> {
                RenderUtil.gradientH(g, bx, by, bx + bw, by + BAR_H, Colors.hsbToRgb(hue, 0, bri), Colors.hsbToRgb(hue, 1, bri));
                value = sat;
            }
            case 2 -> {
                RenderUtil.gradientH(g, bx, by, bx + bw, by + BAR_H, 0xFF000000, Colors.hsbToRgb(hue, sat, 1));
                value = bri;
            }
            default -> {
                g.fill(bx, by, bx + bw, by + BAR_H, 0xFF2A2B30);
                int rgb = Colors.hsbToRgb(hue, sat, bri);
                RenderUtil.gradientH(g, bx, by, bx + bw, by + BAR_H, Colors.withAlpha(rgb, 0), Colors.withAlpha(rgb, 255));
                value = Colors.alpha(setting.get()) / 255f;
            }
        }
        int kx = bx + Math.round(value * bw);
        g.fill(kx - 2, by - 2, kx + 2, by + BAR_H + 2, 0xFF000000);
        g.fill(kx - 1, by - 1, kx + 1, by + BAR_H + 1, 0xFFFFFFFF);
    }

    private void apply(int bar, double mouseX) {
        float v = (float) Math.max(0, Math.min(1, (mouseX - barX()) / barWidth()));
        int alpha = Colors.alpha(setting.get());
        switch (bar) {
            case 0 -> hue = Math.min(v, 0.999f);
            case 1 -> sat = v;
            case 2 -> bri = v;
            default -> alpha = Math.round(v * 255);
        }
        int argb = Colors.withAlpha(Colors.hsbToRgb(hue, sat, bri), alpha);
        lastSynced = setting.allowsAlpha() ? argb : argb | 0xFF000000;
        setting.set(argb);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        if (mouseY < y + Theme.ROW_HEIGHT) {
            expanded = !expanded;
            return true;
        }
        if (!expanded) return false;
        for (int i = 0; i < bars(); i++) {
            int by = barY(i);
            if (mouseY >= by - 3 && mouseY < by + BAR_H + 3) {
                dragBar = i;
                apply(i, mouseX);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (dragBar < 0) return false;
        apply(dragBar, mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = dragBar >= 0;
        dragBar = -1;
        return was;
    }
}

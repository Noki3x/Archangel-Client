package dev.archangelclient.gui.widget;

import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.visual.crosshair.CrosshairSetting;
import dev.archangelclient.visual.crosshair.CrosshairStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

/** Picker for a {@link CrosshairSetting}: every style as a clickable tile under the setting's label. */
public class CrosshairGrid extends SettingWidget<CrosshairSetting> {
    private static final int STEP = Theme.CROSSHAIR_TILE + Theme.CROSSHAIR_TILE_GAP;

    public CrosshairGrid(CrosshairSetting setting) {
        super(setting);
    }

    private int columns() {
        return Math.max(1, (width + Theme.CROSSHAIR_TILE_GAP) / STEP);
    }

    private int rows() {
        return (CrosshairStyle.values().length + columns() - 1) / columns();
    }

    @Override
    public int getHeight() {
        return Theme.ROW_HEIGHT + rows() * STEP;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        String current = setting.get();
        int nameWidth = Fonts.widthBold(current);
        renderLabel(g, nameWidth);
        Fonts.drawBold(g, current, x + width - nameWidth, y + 4, Theme.ACCENT_LIGHT);

        CrosshairStyle[] styles = CrosshairStyle.values();
        for (int i = 0; i < styles.length; i++) {
            int tx = tileX(i), ty = tileY(i);
            boolean selected = styles[i].displayName().equals(current);
            boolean hovered = mouseX >= tx && mouseX < tx + Theme.CROSSHAIR_TILE && mouseY >= ty && mouseY < ty + Theme.CROSSHAIR_TILE;
            int bg = selected ? Theme.ACCENT : hovered ? Theme.CARD_HOVER : Theme.FIELD;
            RenderUtil.roundedRect(g, tx, ty, Theme.CROSSHAIR_TILE, Theme.CROSSHAIR_TILE, Theme.RADIUS, bg);
            int icon = Theme.CROSSHAIR_TILE_ICON, ix = tx + (Theme.CROSSHAIR_TILE - icon) / 2, iy = ty + (Theme.CROSSHAIR_TILE - icon) / 2;
            int t = Theme.CROSSHAIR_TEXTURE_SIZE;
            g.blit(RenderPipelines.GUI_TEXTURED, styles[i].texture(), ix, iy, 0, 0, icon, icon, t, t, t, t, Theme.TEXT);
            if (hovered) g.setTooltipForNextFrame(Fonts.regular(styles[i].displayName()), mouseX, mouseY);
        }
    }

    private int tileX(int i) {
        return x + (i % columns()) * STEP;
    }

    private int tileY(int i) {
        return y + Theme.ROW_HEIGHT + (i / columns()) * STEP;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        CrosshairStyle[] styles = CrosshairStyle.values();
        for (int i = 0; i < styles.length; i++) {
            int tx = tileX(i), ty = tileY(i);
            if (mouseX >= tx && mouseX < tx + Theme.CROSSHAIR_TILE && mouseY >= ty && mouseY < ty + Theme.CROSSHAIR_TILE) {
                setting.set(styles[i].displayName());
                return true;
            }
        }
        return false;
    }
}

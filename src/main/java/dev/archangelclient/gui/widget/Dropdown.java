package dev.archangelclient.gui.widget;

import dev.archangelclient.core.setting.ModeSetting;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/** Dropdown for a {@link ModeSetting}. The option list opens inline, pushing later rows down. */
public class Dropdown extends SettingWidget<ModeSetting> {
    private static final int BOX_WIDTH = 100;
    private static final int BOX_HEIGHT = 16;
    private static final int OPTION_HEIGHT = 14;
    private final Animation open = new Animation(Theme.HOVER_MS, 0f);
    private boolean expanded;

    public Dropdown(ModeSetting setting) {
        super(setting);
    }

    @Override
    public int getHeight() {
        return Theme.ROW_HEIGHT + Math.round(listHeight() * open.get());
    }

    private int listHeight() {
        return setting.getModes().size() * OPTION_HEIGHT + 4;
    }

    private int boxX() {
        return x + width - BOX_WIDTH;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        open.target(expanded ? 1f : 0f);
        renderLabel(g, BOX_WIDTH);
        int bx = boxX(), by = controlY(BOX_HEIGHT);
        boolean overBox = mouseX >= bx && mouseX < bx + BOX_WIDTH && mouseY >= by && mouseY < by + BOX_HEIGHT;
        RenderUtil.roundedRect(g, bx, by, BOX_WIDTH, BOX_HEIGHT, Theme.RADIUS, overBox || expanded ? Theme.CARD_HOVER : Theme.FIELD);
        Fonts.draw(g, Fonts.trim(setting.get(), BOX_WIDTH - 22), bx + 7, by + 4, Theme.TEXT);
        chevron(g, bx + BOX_WIDTH - 12, by + 6, expanded);

        float o = open.get();
        if (o < 0.02f) return;
        int ly = y + Theme.ROW_HEIGHT;
        int visible = Math.round(listHeight() * o);
        g.enableScissor(bx, ly, bx + BOX_WIDTH, ly + visible);
        RenderUtil.roundedRect(g, bx, ly, BOX_WIDTH, listHeight(), Theme.RADIUS, Theme.PANEL_SOLID);
        List<String> modes = setting.getModes();
        for (int i = 0; i < modes.size(); i++) {
            int oy = ly + 2 + i * OPTION_HEIGHT;
            boolean hovered = mouseX >= bx && mouseX < bx + BOX_WIDTH && mouseY >= oy && mouseY < oy + OPTION_HEIGHT;
            if (hovered) RenderUtil.roundedRect(g, bx + 2, oy, BOX_WIDTH - 4, OPTION_HEIGHT, 4, Theme.HOVER_BAR);
            int color = setting.is(modes.get(i)) ? Theme.ACCENT_LIGHT : Colors.fade(Theme.TEXT, o);
            Fonts.draw(g, Fonts.trim(modes.get(i), BOX_WIDTH - 14), bx + 7, oy + 3, color);
        }
        g.disableScissor();
    }

    /** Small V (or ^ when open) drawn from 1 px fills, so it doesn't depend on font glyphs. */
    static void chevron(GuiGraphics g, int x, int y, boolean up) {
        for (int i = 0; i < 3; i++) {
            int row = up ? 2 - i : i;
            g.fill(x + i, y + row, x + 6 - i, y + row + 1, Theme.TEXT_MUTED);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int bx = boxX();
        if (mouseX < bx || mouseX >= bx + BOX_WIDTH) {
            if (expanded && isHovered(mouseX, mouseY)) expanded = false;
            return false;
        }
        int ly = y + Theme.ROW_HEIGHT;
        if (mouseY >= y && mouseY < ly) {
            expanded = !expanded;
            return true;
        }
        if (expanded && mouseY >= ly + 2) {
            int index = (int) ((mouseY - ly - 2) / OPTION_HEIGHT);
            if (index >= 0 && index < setting.getModes().size()) {
                setting.set(setting.getModes().get(index));
                expanded = false;
                return true;
            }
        }
        return false;
    }
}

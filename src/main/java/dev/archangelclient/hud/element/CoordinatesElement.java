package dev.archangelclient.hud.element;

import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ModeSetting;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.hud.HudElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Your block position and the direction you're facing (the same values as the F3 screen). */
public class CoordinatesElement extends HudElement {
    private static final String[] DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private static final String[] AXES = {"+Z", "-X +Z", "-X", "-X -Z", "-Z", "+X -Z", "+X", "+X +Z"};
    private final ModeSetting layout = add(new ModeSetting("Layout", "Stacked lines or a single line", "Stacked", "Stacked", "Single line"));
    private final BooleanSetting direction = add(new BooleanSetting("Direction", "Show the direction you're facing", true));

    public CoordinatesElement() {
        super("Coordinates", "Shows your position and facing direction", 0f, 0.18f, true);
    }

    @Override
    public void render(GuiGraphics g, float partialTick, boolean editor) {
        var player = mc().player;
        if (player == null) return;
        int x = Mth.floor(player.getX()), y = Mth.floor(player.getY()), z = Mth.floor(player.getZ());
        int dir = Math.floorMod(Math.round(Mth.wrapDegrees(player.getYRot()) / 45f), 8);

        List<String[]> rows = new ArrayList<>();
        if (layout.is("Single line")) {
            rows.add(new String[] {"XYZ", x + ", " + y + ", " + z});
        } else {
            rows.add(new String[] {"X", String.valueOf(x)});
            rows.add(new String[] {"Y", String.valueOf(y)});
            rows.add(new String[] {"Z", String.valueOf(z)});
        }
        if (direction.get()) rows.add(new String[] {DIRECTIONS[dir], AXES[dir]});

        int labelW = 0, valueW = 0;
        for (String[] r : rows) {
            labelW = Math.max(labelW, Fonts.width(r[0]));
            valueW = Math.max(valueW, Fonts.width(r[1]));
        }
        width = 5 + labelW + 6 + valueW + 5;
        height = 4 + rows.size() * 10 + 1;
        panel(g, width, height);
        for (int i = 0; i < rows.size(); i++) {
            int ry = 4 + i * 10;
            Fonts.draw(g, rows.get(i)[0], 5, ry, Theme.ACCENT_LIGHT);
            text(g, rows.get(i)[1], 5 + labelW + 6, ry);
        }
    }
}

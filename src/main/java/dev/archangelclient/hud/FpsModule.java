package dev.archangelclient.hud;

import dev.archangelclient.core.event.Render2DEvent;
import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ColorSetting;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.Minecraft;

/**
 * Frame rate counter in the top-left corner. Milestone 3 moves this onto the HUD element system
 * (movable, scalable, editor support); until then it has a fixed position.
 */
public class FpsModule extends Module {
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a translucent panel behind the text", true));
    private final ColorSetting color = add(new ColorSetting("Text color", "Color of the FPS text", Theme.TEXT, false));

    public FpsModule() {
        super("FPS", "Shows your frames per second", Category.HUD, true);
        on(Render2DEvent.class, this::render);
    }

    private void render(Render2DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        var text = Fonts.regular(mc.getFps() + " FPS");
        int w = mc.font.width(text) + 10;
        int x = 4, y = 4;
        if (background.get()) RenderUtil.roundedRect(event.graphics(), x, y, w, 15, Theme.RADIUS, Theme.PANEL);
        event.graphics().drawString(mc.font, text, x + 5, y + 4, color.get(), false);
    }
}

package dev.archangelclient.visual.crosshair;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ColorSetting;
import dev.archangelclient.core.setting.NumberSetting;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Replaces the vanilla crosshair sprite with one of the Archangel styles. Purely cosmetic: it is
 * drawn exactly where and when vanilla draws its crosshair (see {@code GuiMixin}), and the vanilla
 * attack indicator under it is untouched.
 */
public class CrosshairModule extends Module {
    private static final double PERCENT = 100.0;

    public final CrosshairSetting style = add(new CrosshairSetting("Style", "Pick a crosshair", CrosshairStyle.WINGS));
    private final ColorSetting color = add(new ColorSetting("Color", "Crosshair color", Theme.TEXT, false));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", "How see-through the crosshair is",
            100, 10, 100, 5).suffix("%"));
    private final NumberSetting size = add(new NumberSetting("Size", "Relative to the vanilla crosshair",
            100, 50, 300, 10).suffix("%"));
    private final BooleanSetting outline = add(new BooleanSetting("Outline", "Dark edge so it shows on bright blocks", true));
    private final BooleanSetting invert = add(new BooleanSetting("Invert like vanilla",
            "Invert the colors behind it instead of using the color above", false));

    public CrosshairModule() {
        super("Crosshair", "30 custom crosshairs with opacity", Category.VISUALS, true);
    }

    /** Draws the crosshair centered on screen, in place of the vanilla sprite. */
    public void render(GuiGraphics g) {
        CrosshairStyle s = style.style();
        float alpha = (float) (opacity.get() / PERCENT);
        int px = Math.max(1, Math.round(Theme.CROSSHAIR_SIZE * (float) (size.get() / PERCENT)));
        int x = (g.guiWidth() - px) / 2, y = (g.guiHeight() - px) / 2;
        if (invert.get()) {
            // vanilla's crosshair pipeline inverts what's behind it; dimming the color softens that
            int k = Math.round(alpha * 255);
            blit(g, RenderPipelines.CROSSHAIR, s.texture(), x, y, px, ARGB.color(255, k, k, k));
            return;
        }
        if (outline.get()) blit(g, RenderPipelines.GUI_TEXTURED, s.outline(), x, y, px,
                Colors.fade(Theme.CROSSHAIR_OUTLINE, alpha));
        blit(g, RenderPipelines.GUI_TEXTURED, s.texture(), x, y, px, Colors.fade(color.get() | 0xFF000000, alpha));
    }

    /** Draws a whole crosshair texture into a {@code px}-sized square. */
    static void blit(GuiGraphics g, RenderPipeline pipeline, Identifier texture, int x, int y, int px, int color) {
        int t = Theme.CROSSHAIR_TEXTURE_SIZE;
        g.blit(pipeline, texture, x, y, 0, 0, px, px, t, t, t, t, color);
    }
}

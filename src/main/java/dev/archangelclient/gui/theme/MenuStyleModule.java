package dev.archangelclient.gui.theme;

import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ColorSetting;
import dev.archangelclient.core.setting.ModeSetting;
import dev.archangelclient.core.setting.NumberSetting;

/** Look of the animated menu backdrop. When disabled, menus use a plain charcoal gradient. */
public class MenuStyleModule extends Module {
    public final ModeSetting background = add(new ModeSetting("Background", "Backdrop behind Archangel menus",
            "Light rays", "Light rays", "Gradient"));
    public final NumberSetting raySpeed = add(new NumberSetting("Ray speed", "How fast the light rays sway",
            1, 0.25, 3, 0.25).suffix("x")
            .visibleWhen(() -> isRays()));
    public final ColorSetting rayTint = add(new ColorSetting("Ray tint", "Color of the tinted light rays",
            Theme.BEAM_TINT, false).visibleWhen(() -> isRays()));
    public final BooleanSetting vignette = add(new BooleanSetting("Vignette", "Darken the screen edges", true));
    public final BooleanSetting smoothFont = add(new BooleanSetting("Smooth font everywhere",
            "Use the Archangel font in all Minecraft menus, chat and the HUD, not just client menus", true));

    public MenuStyleModule() {
        super("Menu Style", "Animated light rays, vignette and font of the menus", Category.VISUALS, true);
        smoothFont.onChange(FontSharpness::requestReload);
    }

    /** Whether vanilla text should be drawn with the Archangel font instead of the pixel font. */
    public boolean useSmoothFontEverywhere() {
        return isEnabled() && smoothFont.get();
    }

    public boolean isRays() {
        return background.is("Light rays");
    }
}

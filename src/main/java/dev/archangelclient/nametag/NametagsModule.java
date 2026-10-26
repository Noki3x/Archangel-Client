package dev.archangelclient.nametag;

import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.NumberSetting;

/**
 * Nametag look: the Archangel badge next to other Archangel players, their chosen name color,
 * and how nametags are drawn. Only changes how names look; which names are visible stays
 * vanilla (plus, optionally, your own in third person).
 */
public class NametagsModule extends Module {
    public final BooleanSetting badges = add(new BooleanSetting("Archangel badge",
            "Show the wings badge next to players who use Archangel Client", true));
    public final BooleanSetting customNames = add(new BooleanSetting("Custom names",
            "Show Archangel players' display names and name colors", true));
    public final NumberSetting backgroundOpacity = add(new NumberSetting("Background opacity",
            "Darkness of the box behind nametags", 25, 0, 100, 5).suffix("%"));
    public final BooleanSetting textShadow = add(new BooleanSetting("Text shadow",
            "Draw nametag text with a drop shadow", false));
    public final BooleanSetting showOwn = add(new BooleanSetting("Show own nametag",
            "Show your own nametag in third person", false));
    public final NumberSetting scale = add(new NumberSetting("Scale",
            "Size of nametags", 1, 0.5, 2, 0.05).suffix("x"));

    public NametagsModule() {
        super("Nametags", "Archangel badges, custom names and nametag style", Category.NAMETAGS, true);
    }
}

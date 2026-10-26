package dev.archangelclient.sky;

import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ModeSetting;
import dev.archangelclient.core.setting.NumberSetting;

/** Replaces the overworld's night sky (or the whole sky) with a purple nebula. Purely visual. */
public class NebulaSkyModule extends Module {
    public static final String NIGHT = "Night";
    public static final String ALWAYS = "Always";

    public final ModeSetting when = add(new ModeSetting("Show", "Only at night, or day and night", NIGHT, NIGHT, ALWAYS));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", "How strongly the nebula covers the sky",
            1, 0.2, 1, 0.05));
    public final BooleanSetting rotate = add(new BooleanSetting("Rotate with stars", "Turn slowly across the sky over the night", true));

    public NebulaSkyModule() {
        super("Nebula Sky", "A purple nebula night sky", Category.VISUALS, true);
    }
}

package dev.archangelclient.visual.crosshair;

import dev.archangelclient.core.setting.ModeSetting;

/** Choice of crosshair style; shown as a grid of previews instead of a dropdown. */
public class CrosshairSetting extends ModeSetting {
    public CrosshairSetting(String name, String description, CrosshairStyle defaultStyle) {
        super(name, description, defaultStyle.displayName(), CrosshairStyle.displayNames());
    }

    public CrosshairStyle style() {
        return CrosshairStyle.byName(get());
    }
}

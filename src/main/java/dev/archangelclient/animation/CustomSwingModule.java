package dev.archangelclient.animation;

import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.NumberSetting;

/**
 * Speed and easing of your own arm swing animation. Visual only: the swing duration is used for
 * rendering on the client and has no effect on attack timing or cooldown.
 */
public class CustomSwingModule extends Module {
    public final NumberSetting speed = add(new NumberSetting("Swing speed", "How fast your arm swings (1x is vanilla)", 1, 0.5, 2, 0.1).suffix("x"));
    public final BooleanSetting smooth = add(new BooleanSetting("Smooth hit", "Ease the swing in and out for a softer hit animation", false));

    public CustomSwingModule() {
        super("Custom Swing", "Changes the speed and feel of your swing animation", Category.ANIMATIONS, false);
    }

    /** Vanilla swing duration in ticks, adjusted for the speed setting. */
    public int adjustDuration(int ticks) {
        return isEnabled() ? Math.max(1, Math.round(ticks / speed.getFloat())) : ticks;
    }

    /** Swing progress (0..1), eased when smoothing is on. */
    public float adjustProgress(float progress) {
        if (!isEnabled() || !smooth.get() || progress <= 0) return progress;
        return progress * progress * (3 - 2 * progress);
    }
}

package dev.archangelclient.visual;

import dev.archangelclient.core.event.TickEvent;
import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.NumberSetting;
import dev.archangelclient.performance.OptionOverride;
import net.minecraft.client.Minecraft;

/**
 * A brightness slider that drives vanilla's own Brightness option, so it can never go past
 * vanilla's "Bright" (100%). This is deliberately not fullbright: dark areas stay dark.
 */
public class BrightnessModule extends Module {
    private static final double PERCENT = 100.0;

    private final NumberSetting brightness = add(new NumberSetting("Brightness", "0% is Moody, 100% is Bright (vanilla limits)",
            50, 0, 100, 1).suffix("%"));
    private final OptionOverride<Double> gamma = new OptionOverride<>();

    public BrightnessModule() {
        super("Brightness", "Adjust brightness within vanilla's range", Category.VISUALS, false);
        on(TickEvent.class, e -> apply());
        brightness.onChange(() -> {
            if (isEnabled()) apply();
        });
    }

    private void apply() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) gamma.apply(mc.options.gamma(), brightness.get() / PERCENT);
    }

    @Override
    protected void onDisable() {
        gamma.restore();
    }
}

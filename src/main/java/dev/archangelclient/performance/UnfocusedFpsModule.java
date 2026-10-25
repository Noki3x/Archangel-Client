package dev.archangelclient.performance;

import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/**
 * Caps the frame rate while the game window is in the background, to save power and keep other
 * apps responsive. Applied by {@code FramerateLimitTrackerMixin}.
 */
public class UnfocusedFpsModule extends Module {
    private final NumberSetting limit = add(new NumberSetting("FPS limit", "Frame rate cap while the window is unfocused",
            15, 5, 60, 5).suffix(" FPS"));

    public UnfocusedFpsModule() {
        super("Unfocused FPS", "Lowers the frame rate while the game is in the background", Category.PERFORMANCE, true);
    }

    /** The limit to use instead of {@code vanillaLimit}, or {@code vanillaLimit} itself if not applicable. */
    public int apply(int vanillaLimit) {
        if (!isEnabled() || Minecraft.getInstance().isWindowActive()) return vanillaLimit;
        return Math.min(vanillaLimit, limit.getInt());
    }
}

package dev.archangelclient.visual;

import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ColorSetting;
import dev.archangelclient.core.setting.NumberSetting;
import dev.archangelclient.gui.theme.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Avatar;

/**
 * Makes your own player see-through, and optionally outlines your own silhouette so you can find
 * yourself behind blocks in third person. Strictly self-only and cosmetic: it is applied to the
 * camera player's render state alone (see {@code AvatarRendererMixin}), so it never reveals any
 * other player, mob or block.
 */
public class SeeThroughSelfModule extends Module {
    /** Opacity (0..1) for the local player's body this frame; absent for everyone else. */
    public static final RenderStateDataKey<Float> OPACITY = RenderStateDataKey.create(() -> "archangel self opacity");
    private static final double PERCENT = 100.0;

    private final NumberSetting opacity = add(new NumberSetting("Skin opacity", "How solid your own skin looks",
            40, 10, 100, 5).suffix("%"));
    private final BooleanSetting outline = add(new BooleanSetting("Outline yourself",
            "In third person, show your own outline through blocks", true));
    private final ColorSetting outlineColor = add(new ColorSetting("Outline color", "Color of your own outline",
            Theme.ACCENT_LIGHT, false).visibleWhen(outline::get));

    public SeeThroughSelfModule() {
        super("See-Through Self", "Make your own skin transparent", Category.VISUALS, false);
    }

    /** Called while the player renderer fills in a render state; only touches your own player. */
    public void apply(Avatar avatar, AvatarRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || avatar != mc.player || state.isInvisible) {
            state.setData(OPACITY, null);
            return;
        }
        state.setData(OPACITY, (float) (opacity.get() / PERCENT));
        if (outline.get() && !mc.options.getCameraType().isFirstPerson() && state.outlineColor == 0) {
            state.outlineColor = ARGB.opaque(outlineColor.get());
        }
    }
}

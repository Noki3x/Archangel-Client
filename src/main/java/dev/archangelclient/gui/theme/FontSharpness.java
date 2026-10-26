package dev.archangelclient.gui.theme;

import dev.archangelclient.ArchangelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Util;
import java.util.concurrent.CompletableFuture;

/**
 * Keeps the Inter font crisp at any GUI scale.
 *
 * <p>Minecraft rasterizes TTF glyphs once, at {@code size * oversample} pixels, and draws them with
 * nearest-neighbour sampling. If the raster size doesn't match the screen (GUI scale 2 or 3 with a
 * fixed oversample of 4, say) every glyph is resampled and the edges turn jagged. So we rasterize our
 * fonts at exactly the current GUI scale (see {@code TrueTypeGlyphProviderDefinitionMixin}), and
 * reload just the fonts whenever the scale changes.
 */
public final class FontSharpness {
    private FontSharpness() {}

    /** Oversample the Archangel fonts were last loaded with; 0 until the first load. */
    private static volatile int loadedOversample;
    private static boolean reloading;
    private static boolean reloadRequested;

    /** The oversample our TTF fonts should use right now: one font pixel per screen pixel. */
    public static int targetOversample() {
        int scale = Minecraft.getInstance().getWindow().getGuiScale();
        return Math.clamp(scale, 1, Theme.FONT_MAX_OVERSAMPLE);
    }

    /** Called by the font loader mixin for every Archangel TTF it loads. */
    public static float oversampleForLoad() {
        int oversample = targetOversample();
        loadedOversample = oversample;
        return oversample;
    }

    /** Ask for a font reload on the next tick (e.g. after the smooth font setting changes). */
    public static void requestReload() {
        if (loadedOversample != 0) reloadRequested = true; // before the first load there's nothing to redo
    }

    /** Called every client tick; reloads fonts if the GUI scale changed since they were rasterized. */
    public static void tick(Minecraft mc) {
        if (reloading || mc.getOverlay() != null) return; // a full resource reload is already running
        if (!reloadRequested && (loadedOversample == 0 || loadedOversample == targetOversample())) return;
        reloadRequested = false;
        reloading = true;
        var state = new PreparableReloadListener.SharedState(mc.getResourceManager());
        mc.fontManager.reload(state, Util.backgroundExecutor(), CompletableFuture::completedFuture, mc)
                .whenComplete((ignored, error) -> mc.execute(() -> {
                    reloading = false;
                    if (error != null) ArchangelClient.LOGGER.warn("Font reload failed", error);
                }));
    }
}

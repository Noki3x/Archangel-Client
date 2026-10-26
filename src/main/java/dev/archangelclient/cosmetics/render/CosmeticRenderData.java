package dev.archangelclient.cosmetics.render;

import dev.archangelclient.cosmetics.Loadout;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

/**
 * Attached to a player's render state when it is extracted, so the cosmetic layer knows what to
 * draw without touching the entity at render time.
 *
 * @param self     the local player: only they get auras
 * @param preview  rendered in a menu preview rather than the world (no world-space effects)
 * @param entityId key for per-player effect state such as swing trails
 */
public record CosmeticRenderData(Loadout loadout, boolean self, boolean preview, int entityId) {
    public static final RenderStateDataKey<CosmeticRenderData> KEY = RenderStateDataKey.create(() -> "archangel cosmetics");
}

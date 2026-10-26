package dev.archangelclient.cosmetics;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.cosmetics.effect.CosmeticEffects;
import dev.archangelclient.cosmetics.net.CosmeticProfileCache;
import dev.archangelclient.cosmetics.net.CosmeticsApi;
import dev.archangelclient.cosmetics.render.CosmeticRenderData;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Decides who wears what. The local player's loadout comes from the active config profile; other
 * players' loadouts come from the backend through {@link CosmeticProfileCache}.
 *
 * <p>Until accounts exist (milestone 9) every catalog cosmetic counts as owned.
 */
public final class CosmeticManager {
    private final CosmeticRegistry registry = new CosmeticRegistry();
    private final CosmeticProfileCache profiles = new CosmeticProfileCache(new CosmeticsApi());
    private final CosmeticEffects effects = new CosmeticEffects();
    private final Set<String> owned = new HashSet<>();
    private boolean ownsEverything = true;

    public CosmeticRegistry registry() {
        return registry;
    }

    public CosmeticEffects effects() {
        return effects;
    }

    public CosmeticProfileCache profiles() {
        return profiles;
    }

    public boolean isOwned(Cosmetic cosmetic) {
        return ownsEverything || owned.contains(cosmetic.id());
    }

    private CosmeticsModule module() {
        return ArchangelClient.modules().get(CosmeticsModule.class);
    }

    public Loadout localLoadout() {
        return module().loadout();
    }

    public void equip(CosmeticType type, @Nullable String id) {
        module().equip(type, id);
    }

    public boolean isEquipped(Cosmetic cosmetic) {
        return cosmetic.id().equals(localLoadout().get(cosmetic.type()));
    }

    /** Render data for a player in the world, or null when nothing should be drawn for them. */
    public @Nullable CosmeticRenderData dataFor(AbstractClientPlayer player) {
        CosmeticsModule module = module();
        if (!module.isEnabled()) return null;
        boolean self = player == Minecraft.getInstance().player;
        Loadout loadout;
        if (self) {
            if (!module.showOwn.get()) return null;
            loadout = module.loadout();
        } else {
            if (!module.showOthers.get()) return null;
            loadout = profiles.get(player.getUUID());
        }
        if (loadout.isEmpty()) return null;
        return new CosmeticRenderData(loadout, self, false, player.getId());
    }
}

package dev.archangelclient.cosmetics;

import dev.archangelclient.cosmetics.model.CosmeticModel;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * One catalog entry. Textures are original Archangel assets under
 * {@code assets/archangelclient/textures/cosmetic}; models are Blockbench geometry files.
 *
 * @param textures    one texture, or the frames of an animated texture in order
 * @param glowTextures optional emissive overlay per frame (bright parts only), same length as textures
 * @param frameTicks  game ticks each animation frame is shown for
 * @param model       baked model for wings, hats and halos; null for capes and effects
 * @param emissive    drawn at full brightness regardless of world light (glowing cosmetics)
 * @param colors      primary and secondary color, used by auras, trails and hit effects
 */
public record Cosmetic(
        String id,
        String name,
        String description,
        CosmeticType type,
        Rarity rarity,
        List<Identifier> textures,
        List<Identifier> glowTextures,
        int frameTicks,
        @Nullable CosmeticModel model,
        boolean emissive,
        int primaryColor,
        int secondaryColor) {

    public boolean animated() {
        return textures.size() > 1;
    }

    /** Frame shown at {@code ageTicks}; animated textures loop. */
    public int frame(float ageTicks) {
        if (textures.size() <= 1) return 0;
        return Math.floorMod((int) (ageTicks / Math.max(1, frameTicks)), textures.size());
    }

    public @Nullable Identifier texture(float ageTicks) {
        return textures.isEmpty() ? null : textures.get(frame(ageTicks));
    }

    public @Nullable Identifier glowTexture(float ageTicks) {
        return glowTextures.isEmpty() ? null : glowTextures.get(frame(ageTicks) % glowTextures.size());
    }
}

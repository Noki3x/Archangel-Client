package dev.archangelclient.mixin;

import dev.archangelclient.gui.theme.Fonts;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Swaps Minecraft's default pixel font for the Archangel font when "Smooth font everywhere" is on.
 * Only {@code minecraft:default} is swapped; the enchanting and illager alphabets, server resource
 * pack fonts and so on are left alone. Characters Inter lacks still fall back to the default font.
 */
@Mixin(targets = "net.minecraft.client.gui.font.FontManager$CachedFontProvider")
public abstract class CachedFontProviderMixin {
    @ModifyVariable(method = "getGlyphSource", at = @At("HEAD"), argsOnly = true)
    private FontDescription archangel$smoothDefaultFont(FontDescription description) {
        return FontDescription.DEFAULT.equals(description) && Fonts.smoothEverywhere() ? Fonts.INTER : description;
    }
}

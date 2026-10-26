package dev.archangelclient.mixin;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.gui.theme.FontSharpness;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Rasterizes the Archangel TTF fonts at the current GUI scale instead of the JSON's fixed oversample. */
@Mixin(TrueTypeGlyphProviderDefinition.class)
public abstract class TrueTypeGlyphProviderDefinitionMixin {
    @Shadow @Final private Identifier location;

    @ModifyArg(method = "load", index = 3, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/font/TrueTypeGlyphProvider;<init>(Ljava/nio/ByteBuffer;Lorg/lwjgl/util/freetype/FT_Face;FFFFLjava/lang/String;)V"))
    private float archangel$matchGuiScale(float oversample) {
        return ArchangelClient.MOD_ID.equals(location.getNamespace()) ? FontSharpness.oversampleForLoad() : oversample;
    }
}

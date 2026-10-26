package dev.archangelclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.visual.crosshair.CrosshairModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Swaps the vanilla crosshair sprite for the Crosshair module's. Only the sprite itself is replaced
 * (the first blit in renderCrosshair), so vanilla still decides when a crosshair shows and still
 * draws the attack indicator.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @WrapOperation(method = "renderCrosshair", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void archangel$customCrosshair(GuiGraphics g, RenderPipeline pipeline, Identifier sprite, int x, int y,
                                           int w, int h, Operation<Void> original) {
        CrosshairModule module = ArchangelClient.isReady() ? ArchangelClient.modules().get(CrosshairModule.class) : null;
        if (module != null && module.isEnabled()) module.render(g);
        else original.call(g, pipeline, sprite, x, y, w, h);
    }
}

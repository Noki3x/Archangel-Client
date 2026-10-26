package dev.archangelclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.archangelclient.cosmetics.render.CosmeticLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player wearing an Archangel cape gets a matching elytra from the cosmetic layer instead. */
@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void archangel$capeElytra(PoseStack pose, SubmitNodeCollector out, int light, HumanoidRenderState state,
                                      float yRot, float xRot, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar && CosmeticLayer.drawsElytra(avatar)) ci.cancel();
    }
}

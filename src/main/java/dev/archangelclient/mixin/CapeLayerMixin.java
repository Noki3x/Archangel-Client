package dev.archangelclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.cosmetics.CosmeticType;
import dev.archangelclient.cosmetics.render.CosmeticRenderData;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An equipped Archangel cape replaces the vanilla cape instead of z-fighting with it. */
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void archangel$replaceCape(PoseStack pose, SubmitNodeCollector out, int light, AvatarRenderState state,
                                       float yRot, float xRot, CallbackInfo ci) {
        CosmeticRenderData data = state.getData(CosmeticRenderData.KEY);
        if (data != null && ArchangelClient.cosmetics().registry().get(data.loadout().get(CosmeticType.CAPE)) != null) ci.cancel();
    }
}

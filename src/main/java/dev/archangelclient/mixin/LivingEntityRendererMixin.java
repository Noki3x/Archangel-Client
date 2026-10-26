package dev.archangelclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.archangelclient.visual.SeeThroughSelfModule;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * See-Through Self: draws the body translucent, with the module's opacity, when the render state
 * carries {@link SeeThroughSelfModule#OPACITY} (only ever set for your own player).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @ModifyArg(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            index = 2)
    private boolean archangel$translucentSelf(boolean translucent, @Local(argsOnly = true) LivingEntityRenderState state) {
        return translucent || state.getData(SeeThroughSelfModule.OPACITY) != null;
    }

    @ModifyArg(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 6)
    private int archangel$selfOpacity(int color, @Local(argsOnly = true) LivingEntityRenderState state) {
        Float opacity = state.getData(SeeThroughSelfModule.OPACITY);
        return opacity == null ? color : ARGB.multiplyAlpha(color, opacity);
    }
}

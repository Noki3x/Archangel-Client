package dev.archangelclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.nametag.Nametags;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nametag background opacity and scale (the Nametags module). Visibility rules stay vanilla. */
@Mixin(targets = "net.minecraft.client.renderer.feature.NameTagFeatureRenderer$Storage")
public abstract class NameTagStorageMixin {
    private static final String ADD = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V";

    @Redirect(method = ADD, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundOpacity(F)F"))
    private float archangel$opacity(Options options, float fallback) {
        int alpha = ArchangelClient.isReady() ? Nametags.backgroundAlpha() : -1;
        return alpha < 0 ? options.getBackgroundOpacity(fallback) : alpha / 255f;
    }

    @Inject(method = ADD, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", shift = At.Shift.AFTER))
    private void archangel$scale(PoseStack poseStack, Vec3 attachment, int y, Component text, boolean seeThrough, int light,
                                 double distanceSq, CameraRenderState camera, CallbackInfo ci) {
        float scale = ArchangelClient.isReady() ? Nametags.scale() : 1f;
        if (scale != 1f) poseStack.scale(scale, scale, scale);
    }
}

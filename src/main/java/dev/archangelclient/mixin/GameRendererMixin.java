package dev.archangelclient.mixin;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import dev.archangelclient.shader.PostEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs Archangel's post effects on the world image, right where vanilla runs its own post chain. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    private void archangel$postEffects(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PostEffects.INSTANCE.process(minecraft.getMainRenderTarget(), resourcePool);
    }
}

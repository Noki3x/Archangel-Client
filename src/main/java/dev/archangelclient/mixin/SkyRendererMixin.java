package dev.archangelclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.archangelclient.sky.NebulaSky;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the nebula behind the sun, moon and stars of the overworld sky. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void archangel$nebula(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase,
                                  float rainBrightness, float starBrightness, CallbackInfo ci) {
        NebulaSky.render(poseStack, starAngle, rainBrightness, starBrightness);
    }
}

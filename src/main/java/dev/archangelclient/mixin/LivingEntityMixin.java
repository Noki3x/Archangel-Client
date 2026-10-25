package dev.archangelclient.mixin;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.animation.CustomSwingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** Custom swing speed for your own player's arm animation (client rendering only). */
    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void archangel$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if (!ArchangelClient.isReady() || (Object) this != Minecraft.getInstance().player) return;
        cir.setReturnValue(ArchangelClient.modules().get(CustomSwingModule.class).adjustDuration(cir.getReturnValue()));
    }
}

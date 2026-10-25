package dev.archangelclient.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.performance.UnfocusedFpsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void archangel$unfocusedLimit(CallbackInfoReturnable<Integer> cir) {
        if (!ArchangelClient.isReady()) return;
        cir.setReturnValue(ArchangelClient.modules().get(UnfocusedFpsModule.class).apply(cir.getReturnValue()));
    }
}

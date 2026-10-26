package dev.archangelclient.mixin;

import dev.archangelclient.emotes.EmotePose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies emote poses after the vanilla animation. Player, armor and cape models are all humanoid
 * models fed the same render state, so they move together.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void archangel$emote(HumanoidRenderState state, CallbackInfo ci) {
        EmotePose pose = state.getData(EmotePose.KEY);
        if (pose != null) pose.apply((HumanoidModel<?>) (Object) this);
    }
}

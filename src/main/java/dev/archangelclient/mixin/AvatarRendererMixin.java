package dev.archangelclient.mixin;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.animation.OldAnimationsModule;
import dev.archangelclient.cosmetics.render.CosmeticRenderData;
import dev.archangelclient.emotes.EmotePose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    /** Third-person blockhit pose, for your own player only (we only know our own input). */
    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("RETURN"), cancellable = true)
    private static void archangel$blockPose(Avatar avatar, HumanoidArm arm, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (!ArchangelClient.isReady() || avatar != Minecraft.getInstance().player || arm != avatar.getMainArm()) return;
        OldAnimationsModule anim = ArchangelClient.modules().get(OldAnimationsModule.class);
        if (anim.thirdPerson.get() && anim.isBlockhitting()) cir.setReturnValue(HumanoidModel.ArmPose.BLOCK);
    }

    /** Tags each player's render state with what they have equipped, for {@code CosmeticLayer}. */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void archangel$cosmetics(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        CosmeticRenderData data = null;
        if (ArchangelClient.isReady() && avatar instanceof AbstractClientPlayer player) data = ArchangelClient.cosmetics().dataFor(player);
        state.setData(CosmeticRenderData.KEY, data);
        EmotePose emote = null;
        if (ArchangelClient.isReady() && avatar instanceof AbstractClientPlayer player) emote = ArchangelClient.emotes().poseFor(player);
        state.setData(EmotePose.KEY, emote);
    }

    /** An emote's root bone moves the whole player (and everything drawn on it) around the feet. */
    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("TAIL"))
    private void archangel$emoteRoot(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        EmotePose pose = state.getData(EmotePose.KEY);
        if (pose != null) pose.applyRoot(poseStack);
    }
}

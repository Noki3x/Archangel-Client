package dev.archangelclient.mixin;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.animation.OldAnimationsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}

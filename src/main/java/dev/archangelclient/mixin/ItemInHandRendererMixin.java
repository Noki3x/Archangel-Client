package dev.archangelclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.animation.CustomSwingModule;
import dev.archangelclient.animation.ItemPositionModule;
import dev.archangelclient.animation.OldAnimationsModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person animation hooks: 1.7 blockhit / eat / bow poses, old swing (no cooldown dip),
 * smooth swing and item position. Rendering only.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    protected abstract void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equipProgress);

    @Shadow
    protected abstract void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress);

    @Shadow
    protected abstract void applyEatTransform(PoseStack poseStack, float partialTick, HumanoidArm arm, ItemStack stack, Player player);

    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            SubmitNodeCollector collector, int light);

    /** Old swing: ignore the attack cooldown when computing how far the item is raised. */
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float archangel$oldSwing(float swapScale) {
        if (!ArchangelClient.isReady()) return swapScale;
        OldAnimationsModule anim = ArchangelClient.modules().get(OldAnimationsModule.class);
        return anim.isEnabled() && anim.oldSwing.get() ? 1f : swapScale;
    }

    /** Smooth hit: ease the swing progress (third float parameter of renderArmWithItem). */
    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float archangel$smoothSwing(float swingProgress) {
        if (!ArchangelClient.isReady()) return swingProgress;
        return ArchangelClient.modules().get(CustomSwingModule.class).adjustProgress(swingProgress);
    }

    /** Item position offsets, applied in item space right before vanilla draws the held item. */
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void archangel$itemPosition(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
            float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack, SubmitNodeCollector collector,
            int light, CallbackInfo ci) {
        if (ArchangelClient.isReady()) ArchangelClient.modules().get(ItemPositionModule.class).apply(poseStack, arm(player, hand));
    }

    /** 1.7 poses: when one applies, draw the item ourselves and skip vanilla's transform for it. */
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void archangel$oldPoses(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
            float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack, SubmitNodeCollector collector,
            int light, CallbackInfo ci) {
        if (!ArchangelClient.isReady() || stack.isEmpty() || player.isScoping() || stack.is(Items.CROSSBOW)) return;
        OldAnimationsModule anim = ArchangelClient.modules().get(OldAnimationsModule.class);
        if (!anim.isEnabled()) return;

        float swing = ArchangelClient.modules().get(CustomSwingModule.class).adjustProgress(swingProgress);
        HumanoidArm arm = arm(player, hand);
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        boolean using = player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand;
        ItemUseAnimation use = using ? stack.getUseAnimation() : ItemUseAnimation.NONE;

        boolean blockhit = hand == InteractionHand.MAIN_HAND && anim.isBlockhitting();
        boolean eat = anim.oldEat.get() && (use == ItemUseAnimation.EAT || use == ItemUseAnimation.DRINK);
        boolean bow = anim.oldBow.get() && use == ItemUseAnimation.BOW;
        if (!blockhit && !eat && !bow) return;

        poseStack.pushPose();
        if (blockhit) {
            // 1.7: the normal held-item pose with the swing still applied, then the block rotation
            applyItemArmTransform(poseStack, arm, equipProgress);
            applyItemArmAttackTransform(poseStack, arm, swing);
            poseStack.translate(side * -0.14142136F, 0.08F, 0.14142136F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 13.365F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 78.05F));
        } else if (eat) {
            applyEatTransform(poseStack, partialTick, arm, stack, player);
            applyItemArmTransform(poseStack, arm, equipProgress);
            applyItemArmAttackTransform(poseStack, arm, swing);
        } else {
            applyItemArmTransform(poseStack, arm, equipProgress);
            applyItemArmAttackTransform(poseStack, arm, swing);
            bowTransform(poseStack, partialTick, player, stack, side);
        }
        ArchangelClient.modules().get(ItemPositionModule.class).apply(poseStack, arm);
        renderItem(player, stack, side == 1 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                poseStack, collector, light);
        poseStack.popPose();
        ci.cancel();
    }

    /** Vanilla's bow draw transform (unchanged), applied after the 1.7 swing. */
    private static void bowTransform(PoseStack poseStack, float partialTick, Player player, ItemStack stack, int side) {
        poseStack.translate(side * -0.2785682F, 0.18344387F, 0.15731531F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-13.935F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 35.3F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * -9.785F));
        float drawn = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
        float power = drawn / 20.0F;
        power = Math.min(1.0F, (power * power + power * 2.0F) / 3.0F);
        if (power > 0.1F) {
            float shake = Mth.sin((drawn - 0.1F) * 1.3F) * (power - 0.1F);
            poseStack.translate(0.0F, shake * 0.004F, 0.0F);
        }
        poseStack.translate(0.0F, 0.0F, power * 0.04F);
        poseStack.scale(1.0F, 1.0F, 1.0F + power * 0.2F);
        poseStack.mulPose(Axis.YN.rotationDegrees(side * 45.0F));
    }

    private static HumanoidArm arm(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }
}

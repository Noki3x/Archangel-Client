package dev.archangelclient.core.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** Posted after entities render in the world, with the camera-relative pose stack. */
public record Render3DEvent(PoseStack poseStack, MultiBufferSource buffers, float partialTick) {}

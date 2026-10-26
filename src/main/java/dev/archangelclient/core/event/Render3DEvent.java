package dev.archangelclient.core.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Posted while the world is drawn, just before entities are submitted. {@code poseStack} is
 * camera-relative (world position minus camera position); geometry submitted to {@code submitter}
 * is drawn together with entities, with normal depth testing.
 */
public record Render3DEvent(PoseStack poseStack, SubmitNodeCollector submitter, float partialTick) {}

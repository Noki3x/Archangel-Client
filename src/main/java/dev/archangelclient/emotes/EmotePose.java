package dev.archangelclient.emotes;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.emotes.anim.Animation;
import dev.archangelclient.emotes.anim.Bone;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * An emote sampled at one moment, attached to a player's render state. It is applied in
 * {@code HumanoidModel.setupAnim}, so armor and capes (which are humanoid models too) and the
 * cosmetics attached to body parts all follow it.
 *
 * <p>Animated rotations replace the vanilla pose, except the head's, which add to where the
 * player is looking. Positions are offsets. Everything blends with the vanilla pose by
 * {@code weight}.
 */
public final class EmotePose {
    public static final RenderStateDataKey<EmotePose> KEY = RenderStateDataKey.create(() -> ArchangelClient.id("emote_pose").toString());

    private static final int BONES = Bone.values().length;
    private final float[] rotation = new float[BONES * 3];
    private final float[] position = new float[BONES * 3];
    private final boolean[] hasRotation = new boolean[BONES];
    private final boolean[] hasPosition = new boolean[BONES];
    private final float weight;

    private EmotePose(float weight) {
        this.weight = weight;
    }

    public static EmotePose sample(Animation animation, float time, float weight) {
        EmotePose pose = new EmotePose(weight);
        Vector3f v = new Vector3f();
        animation.tracks().forEach((bone, track) -> {
            int i = bone.ordinal();
            if (track.rotation() != null) {
                track.rotation().sample(time, v);
                pose.rotation[i * 3] = v.x * ((float) Math.PI / 180f);
                pose.rotation[i * 3 + 1] = v.y * ((float) Math.PI / 180f);
                pose.rotation[i * 3 + 2] = v.z * ((float) Math.PI / 180f);
                pose.hasRotation[i] = true;
            }
            if (track.position() != null) {
                track.position().sample(time, v);
                pose.position[i * 3] = v.x;
                pose.position[i * 3 + 1] = -v.y; // model y points down
                pose.position[i * 3 + 2] = v.z;
                pose.hasPosition[i] = true;
            }
        });
        return pose;
    }

    public void apply(HumanoidModel<?> model) {
        apply(model.head, Bone.HEAD, true);
        apply(model.body, Bone.BODY, false);
        apply(model.rightArm, Bone.RIGHT_ARM, false);
        apply(model.leftArm, Bone.LEFT_ARM, false);
        apply(model.rightLeg, Bone.RIGHT_LEG, false);
        apply(model.leftLeg, Bone.LEFT_LEG, false);
    }

    private void apply(ModelPart part, Bone bone, boolean additive) {
        int i = bone.ordinal(), o = i * 3;
        if (hasRotation[i]) {
            float rx = rotation[o], ry = rotation[o + 1], rz = rotation[o + 2];
            if (additive) {
                part.xRot += rx * weight;
                part.yRot += ry * weight;
                part.zRot += rz * weight;
            } else {
                part.xRot = lerpAngle(part.xRot, rx);
                part.yRot = lerpAngle(part.yRot, ry);
                part.zRot = lerpAngle(part.zRot, rz);
            }
        }
        if (hasPosition[i]) {
            part.x += position[o] * weight;
            part.y += position[o + 1] * weight;
            part.z += position[o + 2] * weight;
        }
    }

    /** Moves and turns the whole player around the feet; {@code poseStack} is in model space (y down). */
    public void applyRoot(PoseStack poseStack) {
        int i = Bone.ROOT.ordinal(), o = i * 3;
        if (hasPosition[i]) {
            poseStack.translate(position[o] * weight / 16f, position[o + 1] * weight / 16f, position[o + 2] * weight / 16f);
        }
        if (hasRotation[i]) {
            poseStack.mulPose(new Quaternionf().rotationZYX(rotation[o + 2] * weight, rotation[o + 1] * weight, rotation[o] * weight));
        }
    }

    private float lerpAngle(float from, float to) {
        // vanilla angles can wind past ±π (e.g. attack swings); blend along the short way
        float d = to - from;
        d = (float) Math.IEEEremainder(d, Math.PI * 2);
        return weight >= 1 ? to : from + d * weight;
    }
}

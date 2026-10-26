package dev.archangelclient.cosmetics.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Small helpers for writing additive glow quads (position + color). */
public final class GlowGeometry {
    private GlowGeometry() {}

    public static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, int argb) {
        vc.addVertex(pose, x, y, z).setColor(argb);
    }

    public static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vector3f p, int argb) {
        vc.addVertex(pose, p.x, p.y, p.z).setColor(argb);
    }

    /** Camera-facing quad centered on {@code c}, with {@code right}/{@code up} already scaled to half size. */
    public static void billboard(VertexConsumer vc, PoseStack.Pose pose, Vector3f c, Vector3f right, Vector3f up, int argb) {
        vertex(vc, pose, c.x - right.x - up.x, c.y - right.y - up.y, c.z - right.z - up.z, argb);
        vertex(vc, pose, c.x + right.x - up.x, c.y + right.y - up.y, c.z + right.z - up.z, argb);
        vertex(vc, pose, c.x + right.x + up.x, c.y + right.y + up.y, c.z + right.z + up.z, argb);
        vertex(vc, pose, c.x - right.x + up.x, c.y - right.y + up.y, c.z - right.z + up.z, argb);
    }

    /** Diamond-shaped billboard: a quad whose corners sit on the axes, which reads as a soft star. */
    public static void diamond(VertexConsumer vc, PoseStack.Pose pose, Vector3f c, Vector3f right, Vector3f up, int argb) {
        vertex(vc, pose, c.x - right.x, c.y - right.y, c.z - right.z, argb);
        vertex(vc, pose, c.x - up.x, c.y - up.y, c.z - up.z, argb);
        vertex(vc, pose, c.x + right.x, c.y + right.y, c.z + right.z, argb);
        vertex(vc, pose, c.x + up.x, c.y + up.y, c.z + up.z, argb);
    }

    /**
     * Flat annulus in the XZ plane at height {@code y}, colored by angle. Alpha ramps from
     * {@code innerAlpha} at the inner edge to {@code outerAlpha} at the outer edge, which gives
     * soft falloff when drawn as several bands.
     */
    public static void ring(VertexConsumer vc, PoseStack.Pose pose, float y, float inner, float outer, int segments,
                            ColorAt color, float innerAlpha, float outerAlpha) {
        for (int i = 0; i < segments; i++) {
            float a0 = i / (float) segments * Mth.TWO_PI, a1 = (i + 1) / (float) segments * Mth.TWO_PI;
            int c0 = color.at(a0), c1 = color.at(a1);
            float s0 = Mth.sin(a0), k0 = Mth.cos(a0), s1 = Mth.sin(a1), k1 = Mth.cos(a1);
            vertex(vc, pose, k0 * inner, y, s0 * inner, scaleAlpha(c0, innerAlpha));
            vertex(vc, pose, k1 * inner, y, s1 * inner, scaleAlpha(c1, innerAlpha));
            vertex(vc, pose, k1 * outer, y, s1 * outer, scaleAlpha(c1, outerAlpha));
            vertex(vc, pose, k0 * outer, y, s0 * outer, scaleAlpha(c0, outerAlpha));
        }
    }

    public static int scaleAlpha(int argb, float factor) {
        int a = Mth.clamp(Math.round((argb >>> 24) * factor), 0, 255);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    @FunctionalInterface
    public interface ColorAt {
        int at(float angle);
    }
}

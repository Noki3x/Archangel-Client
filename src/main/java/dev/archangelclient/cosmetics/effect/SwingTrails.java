package dev.archangelclient.cosmetics.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.archangelclient.gui.render.Colors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The arc of light behind a swinging blade. While a player swings, the renderer re-poses the arm at
 * several earlier points of the same swing and stores the blade's path here, so the arc is smooth
 * at any frame rate. When the swing ends the last arc fades out.
 */
public final class SwingTrails {
    /** How far back along the swing (in swing progress, 0..1) the arc reaches. */
    public static final float ARC_LENGTH = 0.45f;
    public static final int ARC_STEPS = 12;
    private static final long FADE_MS = 180;

    /** Blade path oldest first: hilt-side and tip-side points per step. */
    public record Arc(List<Vector3f> bases, List<Vector3f> tips, long time) {}

    private final Map<Integer, Arc> arcs = new HashMap<>();

    public void put(int id, List<Vector3f> bases, List<Vector3f> tips) {
        arcs.put(id, new Arc(List.copyOf(bases), List.copyOf(tips), Util.getMillis()));
    }

    /** The current or fading arc for {@code id}, or null once it has faded out. */
    public @Nullable Arc get(int id) {
        Arc arc = arcs.get(id);
        if (arc != null && fade(arc) <= 0) {
            arcs.remove(id);
            return null;
        }
        return arc;
    }

    public void clear() {
        arcs.clear();
    }

    public static float fade(Arc arc) {
        float t = 1f - (Util.getMillis() - arc.time()) / (float) FADE_MS;
        return Math.max(0, Math.min(1, t));
    }

    /** Ribbon along the arc: bright at the blade's edge and at the newest end, fading towards the hilt and the tail. */
    public static void draw(VertexConsumer vc, PoseStack.Pose pose, Arc arc, int primary, int secondary, float intensity) {
        float fade = fade(arc) * intensity;
        int n = arc.tips().size();
        for (int i = 0; i + 1 < n; i++) {
            float fa = ramp(i, n) * fade, fb = ramp(i + 1, n) * fade;
            Vector3f ba = arc.bases().get(i), bb = arc.bases().get(i + 1);
            Vector3f ta = arc.tips().get(i), tb = arc.tips().get(i + 1);
            int ca = Colors.lerp(secondary, primary, fa), cb = Colors.lerp(secondary, primary, fb);
            // wide glow band from hilt to tip
            GlowGeometry.vertex(vc, pose, ba, GlowGeometry.scaleAlpha(ca, fa * 0.12f));
            GlowGeometry.vertex(vc, pose, ta, GlowGeometry.scaleAlpha(ca, fa));
            GlowGeometry.vertex(vc, pose, tb, GlowGeometry.scaleAlpha(cb, fb));
            GlowGeometry.vertex(vc, pose, bb, GlowGeometry.scaleAlpha(cb, fb * 0.12f));
            // hot white core along the edge
            Vector3f ea = new Vector3f(ba).lerp(ta, 0.8f), eb = new Vector3f(bb).lerp(tb, 0.8f);
            GlowGeometry.vertex(vc, pose, ea, 0x00FFFFFF);
            GlowGeometry.vertex(vc, pose, ta, GlowGeometry.scaleAlpha(0xFFEAF2FF, fa));
            GlowGeometry.vertex(vc, pose, tb, GlowGeometry.scaleAlpha(0xFFEAF2FF, fb));
            GlowGeometry.vertex(vc, pose, eb, 0x00FFFFFF);
        }
    }

    private static float ramp(int i, int n) {
        float t = i / (float) (n - 1);
        return (float) Math.pow(t, 1.3);
    }
}

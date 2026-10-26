package dev.archangelclient.cosmetics.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.archangelclient.cosmetics.render.CosmeticRenderTypes;
import dev.archangelclient.gui.render.Colors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A tiny client-side particle system for cosmetic light effects: hit sparks, the flash at the hit
 * point and aura motes. Rendered as additive quads with normal depth testing, so like vanilla
 * particles they never show through walls. Purely visual; nothing here reads or changes game state.
 */
public final class GlowParticles {
    private static final int MAX_PARTICLES = 800;

    private enum Kind { SPARK, FLASH, MOTE }

    private static final class Particle {
        final Kind kind;
        double x, y, z, prevX, prevY, prevZ, vx, vy, vz;
        final int life;
        final int color;
        final float size;
        final double drag, gravity;
        int age;

        Particle(Kind kind, Vec3 pos, Vec3 velocity, int life, int color, float size, double drag, double gravity) {
            this.kind = kind;
            x = prevX = pos.x;
            y = prevY = pos.y;
            z = prevZ = pos.z;
            vx = velocity.x;
            vy = velocity.y;
            vz = velocity.z;
            this.life = life;
            this.color = color;
            this.size = size;
            this.drag = drag;
            this.gravity = gravity;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final RandomSource random = RandomSource.create();

    public void tick() {
        particles.removeIf(p -> {
            p.prevX = p.x;
            p.prevY = p.y;
            p.prevZ = p.z;
            p.x += p.vx;
            p.y += p.vy;
            p.z += p.vz;
            p.vx *= p.drag;
            p.vy = p.vy * p.drag - p.gravity;
            p.vz *= p.drag;
            return ++p.age >= p.life;
        });
    }

    public void clear() {
        particles.clear();
    }

    /** A burst of sparks flying out of {@code at}, biased along {@code normal} (towards the attacker). */
    public void spawnSparks(Vec3 at, Vec3 normal, int primary, int secondary, int count) {
        Vec3 n = normal.lengthSqr() > 1e-6 ? normal.normalize() : new Vec3(0, 1, 0);
        for (int i = 0; i < count; i++) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            if (dir.dot(n) < 0) dir = dir.subtract(n.scale(2 * dir.dot(n))); // reflect into the front hemisphere
            dir = dir.add(n.scale(0.35)).normalize();
            double speed = 0.1 + random.nextDouble() * 0.2;
            int color = Colors.lerp(primary, secondary, random.nextFloat() * 0.6f);
            add(new Particle(Kind.SPARK, at, dir.scale(speed), 8 + random.nextInt(7), color,
                    0.028f + random.nextFloat() * 0.022f, 0.84, 0.01));
        }
        add(new Particle(Kind.FLASH, at, Vec3.ZERO, 6, primary, 0.45f, 1, 0));
    }

    /** One slow rising mote, used by auras. */
    public void spawnMote(Vec3 at, int color) {
        Vec3 v = new Vec3((random.nextDouble() - 0.5) * 0.01, 0.015 + random.nextDouble() * 0.02, (random.nextDouble() - 0.5) * 0.01);
        add(new Particle(Kind.MOTE, at, v, 22 + random.nextInt(14), color, 0.035f + random.nextFloat() * 0.03f, 0.98, -0.0004));
    }

    private void add(Particle p) {
        if (particles.size() >= MAX_PARTICLES) particles.remove(0);
        particles.add(p);
    }

    public boolean isEmpty() {
        return particles.isEmpty();
    }

    /** Submits every live particle; {@code poseStack} is the camera-relative world pose. */
    public void render(PoseStack poseStack, SubmitNodeCollector out, Camera camera, float partialTick) {
        if (particles.isEmpty()) return;
        Vec3 cam = camera.position();
        Quaternionf rotation = new Quaternionf(camera.rotation());
        List<Particle> snapshot = List.copyOf(particles);
        out.submitCustomGeometry(poseStack, CosmeticRenderTypes.GLOW, (pose, vc) -> {
            Vector3f right = new Vector3f(), up = new Vector3f(), c = new Vector3f();
            for (Particle p : snapshot) {
                float t = (p.age + partialTick) / p.life;
                if (t >= 1) continue;
                c.set((float) (Mth.lerp(partialTick, p.prevX, p.x) - cam.x),
                        (float) (Mth.lerp(partialTick, p.prevY, p.y) - cam.y),
                        (float) (Mth.lerp(partialTick, p.prevZ, p.z) - cam.z));
                switch (p.kind) {
                    case SPARK -> spark(vc, pose, p, c, t);
                    case FLASH -> {
                        float s = p.size * (0.6f + t);
                        int color = GlowGeometry.scaleAlpha(Colors.lerp(0xFFFFFFFF, p.color, t), (1 - t) * 0.9f);
                        rotation.transform(right.set(s, 0, 0));
                        rotation.transform(up.set(0, s, 0));
                        GlowGeometry.diamond(vc, pose, c, right, up, color);
                        rotation.transform(right.set(s * 0.45f, 0, 0));
                        rotation.transform(up.set(0, s * 0.45f, 0));
                        GlowGeometry.billboard(vc, pose, c, right, up, GlowGeometry.scaleAlpha(0xFFFFFFFF, (1 - t) * 0.6f));
                    }
                    case MOTE -> {
                        float fade = Mth.sin(t * Mth.PI);
                        float s = p.size * (0.7f + 0.3f * fade);
                        rotation.transform(right.set(s, 0, 0));
                        rotation.transform(up.set(0, s, 0));
                        GlowGeometry.diamond(vc, pose, c, right, up, GlowGeometry.scaleAlpha(p.color, fade * 0.85f));
                        rotation.transform(right.set(s * 0.35f, 0, 0));
                        rotation.transform(up.set(0, s * 0.35f, 0));
                        GlowGeometry.billboard(vc, pose, c, right, up, GlowGeometry.scaleAlpha(0xFFFFFFFF, fade * 0.5f));
                    }
                }
            }
        });
    }

    /** A streak from the spark's head back along its velocity, narrowing to the tail. */
    private static void spark(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose, Particle p, Vector3f head, float t) {
        Vector3f dir = new Vector3f((float) p.vx, (float) p.vy, (float) p.vz);
        float len = dir.length();
        if (len < 1e-4f) return;
        dir.mul(Math.min(0.45f, len * 2.6f) / len);
        Vector3f tail = new Vector3f(head).sub(dir);
        // widen perpendicular to both the streak and the view ray so it always faces the camera
        Vector3f side = new Vector3f(dir).cross(head);
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize().mul(p.size * (1 - t * 0.5f));
        int core = GlowGeometry.scaleAlpha(Colors.lerp(0xFFFFFFFF, p.color, Math.min(1, t * 1.6f)), 1 - t);
        int end = GlowGeometry.scaleAlpha(p.color, 0f);
        GlowGeometry.vertex(vc, pose, head.x - side.x, head.y - side.y, head.z - side.z, core);
        GlowGeometry.vertex(vc, pose, head.x + side.x, head.y + side.y, head.z + side.z, core);
        GlowGeometry.vertex(vc, pose, tail.x + side.x * 0.3f, tail.y + side.y * 0.3f, tail.z + side.z * 0.3f, end);
        GlowGeometry.vertex(vc, pose, tail.x - side.x * 0.3f, tail.y - side.y * 0.3f, tail.z - side.z * 0.3f, end);
    }
}

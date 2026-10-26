package dev.archangelclient.cosmetics.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.cosmetics.Cosmetic;
import dev.archangelclient.cosmetics.CosmeticType;
import dev.archangelclient.cosmetics.CosmeticsModule;
import dev.archangelclient.cosmetics.render.CosmeticRenderTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * World-space cosmetic effects that aren't part of the player model: hit sparks, aura motes,
 * afterimage history and the first-person swing trail. Only reacts to what the local player
 * already sees (their own attacks and movement) and only draws decoration.
 */
public final class CosmeticEffects {
    /** Trail key for the first-person arc, which lives in camera space instead of model space. */
    private static final int FIRST_PERSON_TRAIL = Integer.MIN_VALUE;

    private final GlowParticles particles = new GlowParticles();
    private final Afterimages afterimages = new Afterimages();
    private final SwingTrails trails = new SwingTrails();
    private final RandomSource random = RandomSource.create();

    public Afterimages afterimages() {
        return afterimages;
    }

    public SwingTrails trails() {
        return trails;
    }

    private static CosmeticsModule module() {
        return ArchangelClient.modules().get(CosmeticsModule.class);
    }

    private static @Nullable Cosmetic equipped(CosmeticType type) {
        CosmeticsModule module = module();
        if (!module.isEnabled() || !module.showOwn.get()) return null;
        return ArchangelClient.cosmetics().registry().get(module.loadout().get(type));
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SPEARS)
                || stack.is(Items.TRIDENT) || stack.is(Items.MACE);
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            particles.clear();
            afterimages.tick(null);
            trails.clear();
            return;
        }
        if (mc.isPaused()) return;
        particles.tick();
        afterimages.tick(player);

        Cosmetic aura = equipped(CosmeticType.AURA);
        if (aura != null && module().auraParticles.get() && !mc.options.getCameraType().isFirstPerson() && !player.isInvisible()) {
            int count = random.nextFloat() < module().auraIntensity.get() * 0.8 ? 2 : 1;
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = 0.45 + random.nextDouble() * 0.3;
                Vec3 at = player.position().add(Math.cos(angle) * r, random.nextDouble() * 1.7, Math.sin(angle) * r);
                particles.spawnMote(at, random.nextBoolean() ? aura.primaryColor() : aura.secondaryColor());
            }
        }
    }

    /** The local player hit {@code target}: burst sparks where the crosshair ray meets its hitbox. */
    public void onAttack(Entity target) {
        Cosmetic effect = equipped(CosmeticType.HIT_EFFECT);
        LocalPlayer player = Minecraft.getInstance().player;
        if (effect == null || player == null) return;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1f);
        AABB box = target.getBoundingBox().inflate(target.getPickRadius());
        Vec3 hit = box.clip(eye, eye.add(look.scale(8))).orElse(box.getCenter());
        particles.spawnSparks(hit, look.reverse(), effect.primaryColor(), effect.secondaryColor(), module().sparkCount.get().intValue());
    }

    public void render(PoseStack poseStack, SubmitNodeCollector out, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        particles.render(poseStack, out, camera, partialTick);
        renderFirstPersonTrail(mc, poseStack, out, camera, partialTick);
    }

    /**
     * In first person the player model isn't drawn, so the trail is a crescent swept in front of
     * the camera following the vanilla swing progress.
     */
    private void renderFirstPersonTrail(Minecraft mc, PoseStack poseStack, SubmitNodeCollector out, Camera camera, float partialTick) {
        LocalPlayer player = mc.player;
        Cosmetic trail = equipped(CosmeticType.TRAIL);
        if (player == null || trail == null || !module().firstPersonTrail.get() || !mc.options.getCameraType().isFirstPerson()) return;
        float swing = player.getAttackAnim(partialTick);
        boolean weaponOk = !module().weaponsOnly.get() || isWeapon(player.getMainHandItem());
        if (swing > 0 && weaponOk) {
            float side = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? -1 : 1;
            float start = Math.max(0, swing - SwingTrails.ARC_LENGTH);
            List<Vector3f> bases = new ArrayList<>(), tips = new ArrayList<>();
            for (int i = 0; i <= SwingTrails.ARC_STEPS; i++) {
                float p = start + (swing - start) * i / SwingTrails.ARC_STEPS;
                float e = 1 - (1 - p) * (1 - p);
                float theta = (35f - 190f * e) * Mth.DEG_TO_RAD;
                float cx = Mth.cos(theta) * side, cy = Mth.sin(theta) * 0.75f;
                tips.add(new Vector3f(cx * 0.85f + 0.08f * side, cy * 0.85f - 0.05f, -1.3f));
                bases.add(new Vector3f(cx * 0.45f + 0.08f * side, cy * 0.45f - 0.05f, -1.2f));
            }
            trails.put(FIRST_PERSON_TRAIL, bases, tips);
        }
        SwingTrails.Arc arc = trails.get(FIRST_PERSON_TRAIL);
        if (arc == null) return;
        Quaternionf rotation = new Quaternionf(camera.rotation());
        poseStack.pushPose();
        poseStack.mulPose(rotation);
        out.submitCustomGeometry(poseStack, CosmeticRenderTypes.GLOW,
                (pose, vc) -> SwingTrails.draw(vc, pose, arc, trail.primaryColor(), trail.secondaryColor(), 0.8f));
        poseStack.popPose();
    }
}

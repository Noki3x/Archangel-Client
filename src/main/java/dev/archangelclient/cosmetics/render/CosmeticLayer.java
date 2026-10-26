package dev.archangelclient.cosmetics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.cosmetics.Cosmetic;
import dev.archangelclient.cosmetics.CosmeticRegistry;
import dev.archangelclient.cosmetics.CosmeticType;
import dev.archangelclient.cosmetics.CosmeticsModule;
import dev.archangelclient.cosmetics.Loadout;
import dev.archangelclient.cosmetics.effect.Afterimages;
import dev.archangelclient.cosmetics.effect.CosmeticEffects;
import dev.archangelclient.cosmetics.effect.GlowGeometry;
import dev.archangelclient.cosmetics.effect.SwingTrails;
import dev.archangelclient.cosmetics.model.CosmeticModel;
import dev.archangelclient.gui.render.Colors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Feature renderer added to the player renderer that draws equipped cosmetics: capes, Blockbench
 * model cosmetics (wings, hats, halos), the local player's aura and swing trails.
 *
 * <p>Everything is submitted through the normal entity pipeline with depth testing, so cosmetics are
 * hidden by walls exactly like the player wearing them, and nothing is drawn for invisible players.
 */
public class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** Model space is scaled by this in {@code AvatarRenderer.scale}. */
    private static final float PLAYER_SCALE = 0.9375f;
    /** Feet height in model space (Y points down from the neck). */
    private static final float FEET_Y = 1.5f;
    private static final float[] SHELL_SCALES = {1.06f, 1.12f, 1.19f};
    private static final float[] SHELL_ALPHA = {0.2f, 0.13f, 0.07f};

    /** Vanilla's elytra offset from the body (WingsLayer). */
    private static final float ELYTRA_Z = 0.125f;

    private final PlayerCapeModel capeModel;
    private final ElytraModel elytraModel;
    private final ElytraModel elytraBabyModel;

    public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
        super(parent);
        this.capeModel = new PlayerCapeModel(models.bakeLayer(ModelLayers.PLAYER_CAPE));
        this.elytraModel = new ElytraModel(models.bakeLayer(ModelLayers.ELYTRA));
        this.elytraBabyModel = new ElytraModel(models.bakeLayer(ModelLayers.ELYTRA_BABY));
    }

    /**
     * Whether this layer draws the elytra itself, in the colors of the equipped cape (vanilla's
     * WingsLayer skips the player then, see {@code WingsLayerMixin}).
     */
    public static boolean drawsElytra(AvatarRenderState state) {
        CosmeticRenderData data = state.getData(CosmeticRenderData.KEY);
        return data != null && state.showCape && wearsGlider(state) && ArchangelClient.isReady()
                && ArchangelClient.cosmetics().registry().get(data.loadout().get(CosmeticType.CAPE)) != null;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector out, int light, AvatarRenderState state, float yRot, float xRot) {
        CosmeticRenderData data = state.getData(CosmeticRenderData.KEY);
        if (data == null || state.isInvisible || !ArchangelClient.isReady()) return;
        CosmeticRegistry registry = ArchangelClient.cosmetics().registry();
        Loadout loadout = data.loadout();

        Cosmetic cape = registry.get(loadout.get(CosmeticType.CAPE));
        if (cape != null) {
            submitCape(pose, out, light, state, cape);
            if (drawsElytra(state)) submitElytra(pose, out, light, state, cape);
        }
        for (CosmeticType type : new CosmeticType[] {CosmeticType.WINGS, CosmeticType.HAT, CosmeticType.HALO}) {
            Cosmetic c = registry.get(loadout.get(type));
            if (c != null && c.model() != null) submitModelCosmetic(pose, out, light, state, c);
        }
        Cosmetic aura = registry.get(loadout.get(CosmeticType.AURA));
        if (aura != null && data.self()) submitAura(pose, out, light, state, data, aura);
        Cosmetic trail = registry.get(loadout.get(CosmeticType.TRAIL));
        if (trail != null) submitTrail(pose, out, state, data, trail);
    }

    private static boolean wearsGlider(AvatarRenderState state) {
        return state.chestEquipment.has(DataComponents.GLIDER);
    }

    // --- capes

    /** The elytra, textured with the cape's current frame (the cape textures carry elytra UVs too). */
    private void submitElytra(PoseStack pose, SubmitNodeCollector out, int light, AvatarRenderState state, Cosmetic cape) {
        Identifier texture = cape.texture(state.ageInTicks);
        if (texture == null) return;
        ElytraModel model = state.isBaby ? elytraBabyModel : elytraModel;
        pose.pushPose();
        pose.translate(0.0F, 0.0F, ELYTRA_Z);
        out.submitModel(model, state, pose, RenderTypes.armorCutoutNoCull(texture), light, OverlayTexture.NO_OVERLAY,
                -1, null, state.outlineColor, null);
        Identifier glow = cape.glowTexture(state.ageInTicks);
        if (glow != null) {
            out.submitModel(model, state, pose, RenderTypes.eyes(glow), light, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
        }
        if (state.chestEquipment.hasFoil()) {
            out.submitModel(model, state, pose, RenderTypes.armorEntityGlint(), light, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
        }
        pose.popPose();
    }

    private void submitCape(PoseStack pose, SubmitNodeCollector out, int light, AvatarRenderState state, Cosmetic cape) {
        if (!state.showCape || wearsGlider(state)) return;
        Identifier texture = cape.texture(state.ageInTicks);
        if (texture == null) return;
        pose.pushPose();
        // same offset vanilla uses so the cape clears a chestplate
        if (!state.chestEquipment.isEmpty()) pose.translate(0.0F, -0.053125F, 0.06875F);
        out.submitModel(capeModel, state, pose, RenderTypes.entitySolid(texture), light, OverlayTexture.NO_OVERLAY,
                -1, null, state.outlineColor, null);
        Identifier glow = cape.glowTexture(state.ageInTicks);
        if (glow != null) {
            out.submitModel(capeModel, state, pose, RenderTypes.eyes(glow), light, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
        }
        pose.popPose();
    }

    // --- wings, hats, halos

    private void submitModelCosmetic(PoseStack pose, SubmitNodeCollector out, int light, AvatarRenderState state, Cosmetic c) {
        CosmeticModel model = c.model();
        Identifier texture = c.texture(state.ageInTicks);
        if (texture == null) return;
        if (c.type() == CosmeticType.WINGS && (wearsGlider(state) || state.isFallFlying)) return;

        pose.pushPose();
        ModelPart anchor = model.attach() == CosmeticModel.Attach.HEAD ? getParentModel().head : getParentModel().body;
        anchor.translateAndRotate(pose);
        if (model.attach() == CosmeticModel.Attach.HEAD && !state.headEquipment.isEmpty()) {
            // sit on top of a helmet instead of inside it
            pose.scale(1.13f, 1.13f, 1.13f);
        }
        if (c.type() == CosmeticType.WINGS && !state.chestEquipment.isEmpty()) pose.translate(0, 0, 1 / 16f);

        // emissive cosmetics skip world light and face shading so they stay luminous in the dark
        RenderType base = c.emissive() ? CosmeticRenderTypes.emissive(texture) : RenderTypes.entityTranslucent(texture);
        out.submitModel(model, state, pose, base, light, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
        Identifier glow = c.glowTexture(state.ageInTicks);
        if (glow != null) {
            float pulse = 0.4f + 0.12f * Mth.sin(state.ageInTicks * 0.12f);
            out.submitModel(model, state, pose, CosmeticRenderTypes.shell(glow, 0, 0), light, OverlayTexture.NO_OVERLAY,
                    GlowGeometry.scaleAlpha(0xFFFFFFFF, pulse), null, 0, null);
        }
        pose.popPose();
    }

    // --- aura

    private void submitAura(PoseStack pose, SubmitNodeCollector out, int light, AvatarRenderState state, CosmeticRenderData data, Cosmetic aura) {
        CosmeticsModule module = ArchangelClient.modules().get(CosmeticsModule.class);
        float intensity = module.auraIntensity.get().floatValue();
        float age = state.ageInTicks;
        Identifier swirl = aura.texture(age);
        int primary = aura.primaryColor(), secondary = aura.secondaryColor();

        // glowing silhouette: a few inflated additive copies of the player, brightest closest in
        if (swirl != null) {
            for (int i = 0; i < SHELL_SCALES.length; i++) {
                float s = SHELL_SCALES[i] + 0.012f * Mth.sin(age * 0.2f + i);
                // the menu preview sits on a dark backdrop where additive light reads much stronger
                float boost = data.preview() ? 1f : 1.6f;
                int color = GlowGeometry.scaleAlpha(Colors.lerp(primary, secondary, i / 2f), SHELL_ALPHA[i] * intensity * boost);
                pose.pushPose();
                pose.translate(0, 0.75f, 0);
                pose.scale(s, s, s);
                pose.translate(0, -0.75f, 0);
                out.submitModel(getParentModel(), state, pose, CosmeticRenderTypes.shell(swirl, age * 0.006f * (i + 1), age * 0.011f),
                        light, OverlayTexture.NO_OVERLAY, color, null, 0, null);
                pose.popPose();
            }
        }

        if (module.afterimages.get() && !data.preview()) submitAfterimages(pose, out, light, state, module, primary, secondary, intensity);

        if (module.auraRing.get()) {
            float pulse = 1 + 0.05f * Mth.sin(age * 0.15f);
            float spin = age * 0.04f;
            GlowGeometry.ColorAt color = a -> Colors.lerp(primary, secondary, 0.5f + 0.5f * Mth.sin(a * 2 + spin * 3));
            out.submitCustomGeometry(pose, CosmeticRenderTypes.GLOW, (p, vc) -> {
                float y = FEET_Y - 0.02f;
                float r = 0.62f * pulse;
                GlowGeometry.ring(vc, p, y, r - 0.32f, r - 0.05f, 48, color, 0f, 0.28f * intensity);
                GlowGeometry.ring(vc, p, y, r - 0.05f, r, 48, color, 0.28f * intensity, 0.95f * intensity);
                GlowGeometry.ring(vc, p, y, r, r + 0.05f, 48, color, 0.95f * intensity, 0.3f * intensity);
                GlowGeometry.ring(vc, p, y, r + 0.05f, r + 0.3f, 48, color, 0.3f * intensity, 0f);
                // faint outer ripple expanding every two seconds
                float ripple = (age % 40) / 40f;
                float rr = r + 0.1f + ripple * 0.7f;
                float ra = (1 - ripple) * 0.45f * intensity;
                GlowGeometry.ring(vc, p, y, rr - 0.04f, rr, 48, color, 0f, ra);
                GlowGeometry.ring(vc, p, y, rr, rr + 0.04f, 48, color, ra, 0f);
            });
        }
    }

    /**
     * Holographic copies of the player at earlier positions. History is in world space, so each
     * offset is mapped back through the renderer's rotation and mirror into model space.
     */
    private void submitAfterimages(PoseStack pose, SubmitNodeCollector out, int light, AvatarRenderState state, CosmeticsModule module,
                                   int primary, int secondary, float intensity) {
        Afterimages history = ArchangelClient.cosmetics().effects().afterimages();
        int count = module.afterimageCount.get().intValue();
        Quaternionf undoBody = new Quaternionf().rotationY((180f - state.bodyRot) * Mth.DEG_TO_RAD).conjugate();
        Identifier skin = state.skin.body().texturePath();
        for (int i = 1; i <= count; i++) {
            Afterimages.Sample s = history.get(i * Afterimages.SPACING);
            if (s == null) break;
            Vector3f d = new Vector3f((float) (s.x() - state.x), (float) (s.y() - state.y), (float) (s.z() - state.z));
            if (d.lengthSquared() < 0.04f) continue; // standing still: nothing trails behind
            undoBody.transform(d);
            d.set(-d.x, -d.y, d.z).div(PLAYER_SCALE * state.scale);
            float fade = (1f - (i - 1f) / count) * 0.24f * intensity;
            int color = GlowGeometry.scaleAlpha(Colors.lerp(primary, secondary, i / (float) count), fade);
            pose.pushPose();
            pose.translate(d.x, d.y, d.z);
            pose.mulPose(Axis.YP.rotationDegrees(s.bodyRot() - state.bodyRot));
            out.submitModel(getParentModel(), state, pose, CosmeticRenderTypes.shell(skin, 0, state.ageInTicks * 0.02f),
                    light, OverlayTexture.NO_OVERLAY, color, null, 0, null);
            pose.popPose();
        }
    }

    // --- swing trail

    private void submitTrail(PoseStack pose, SubmitNodeCollector out, AvatarRenderState state, CosmeticRenderData data, Cosmetic trail) {
        CosmeticsModule module = ArchangelClient.modules().get(CosmeticsModule.class);
        SwingTrails trails = ArchangelClient.cosmetics().effects().trails();
        // item tags aren't bound on the title screen, and the preview always holds a sword anyway
        boolean weaponOk = data.preview() || !module.weaponsOnly.get() || CosmeticEffects.isWeapon(state.getMainHandItemStack());
        if (state.attackTime > 0 && weaponOk) {
            // re-pose the arm at earlier moments of this swing to trace the blade's path
            float now = state.attackTime, start = Math.max(0, now - SwingTrails.ARC_LENGTH);
            List<Vector3f> bases = new ArrayList<>(), tips = new ArrayList<>();
            for (int i = 0; i <= SwingTrails.ARC_STEPS; i++) {
                state.attackTime = start + (now - start) * i / SwingTrails.ARC_STEPS;
                getParentModel().setupAnim(state);
                Vector3f[] blade = bladePoints(state);
                bases.add(blade[0]);
                tips.add(blade[1]);
            }
            state.attackTime = now;
            getParentModel().setupAnim(state);
            trails.put(data.entityId(), bases, tips);
        }
        SwingTrails.Arc arc = trails.get(data.entityId());
        if (arc == null) return;
        out.submitCustomGeometry(pose, CosmeticRenderTypes.GLOW,
                (p, vc) -> SwingTrails.draw(vc, p, arc, trail.primaryColor(), trail.secondaryColor(), 1f));
    }

    /**
     * Where the held blade's hilt end and tip are this frame, in model space: the same transforms
     * the held-item layer applies (hand, then the handheld third-person display transform).
     */
    private Vector3f[] bladePoints(AvatarRenderState state) {
        HumanoidArm arm = state.mainArm;
        boolean left = arm == HumanoidArm.LEFT;
        PoseStack ps = new PoseStack();
        getParentModel().translateToHand(state, arm, ps);
        ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
        ps.mulPose(Axis.YP.rotationDegrees(180.0F));
        ps.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        // vanilla handheld item, thirdperson_righthand: rotation [0, -90, 55], translation [0, 4, 0.5], scale 0.85
        ps.translate(0, 4 / 16f, 0.5f / 16f);
        ps.mulPose(new Quaternionf().rotationXYZ(0, (left ? 90 : -90) * Mth.DEG_TO_RAD, (left ? -55 : 55) * Mth.DEG_TO_RAD));
        ps.scale(0.85f, 0.85f, 0.85f);
        // item model space spans -0.5..0.5 here; a sword's sprite runs from hilt (bottom left) to tip (top right)
        Vector3f base = ps.last().pose().transformPosition(new Vector3f(-0.12f, -0.12f, 0));
        Vector3f tip = ps.last().pose().transformPosition(new Vector3f(0.5f, 0.5f, 0));
        return new Vector3f[] {base, tip};
    }
}

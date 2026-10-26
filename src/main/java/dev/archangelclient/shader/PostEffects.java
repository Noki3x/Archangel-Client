package dev.archangelclient.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.renderer.UniformValue;
import dev.archangelclient.ArchangelClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * Runs the {@link PostEffectsModule} effects on the world image each frame. The chain is built
 * in code from the current settings (uniform values are baked into a post chain), and rebuilt
 * only when a setting changes. Shaders live in {@code assets/archangelclient/shaders/post/}.
 */
public final class PostEffects {
    public static final PostEffects INSTANCE = new PostEffects();

    private static final Identifier MAIN = PostChain.MAIN_TARGET_ID;
    private static final Identifier SWAP_A = ArchangelClient.id("swap_a");
    private static final Identifier SWAP_B = ArchangelClient.id("swap_b");
    private static final Identifier BLOOM_A = ArchangelClient.id("bloom_a");
    private static final Identifier BLOOM_B = ArchangelClient.id("bloom_b");
    private static final Identifier PREVIOUS = ArchangelClient.id("motion_previous");
    private static final Identifier SCREENQUAD = Identifier.withDefaultNamespace("core/screenquad");
    private static final Identifier BLIT = Identifier.withDefaultNamespace("post/blit");
    /** Bloom softness: the bright-pass is blurred twice, the second time this much wider. */
    private static final float BLOOM_SECOND_PASS = 2.5f;

    private final CachedOrthoProjectionMatrixBuffer projection = new CachedOrthoProjectionMatrixBuffer("archangel post effects", 0.1f, 1000f, false);
    private @Nullable PostChain chain;
    private @Nullable List<Object> builtFor;
    private boolean failed;

    private PostEffects() {}

    /** Called once per frame after the world is drawn, before the HUD. */
    public void process(RenderTarget main, GraphicsResourceAllocator allocator) {
        PostEffectsModule module = ArchangelClient.modules().get(PostEffectsModule.class);
        if (!module.isEnabled() || IrisBridge.isShaderPackInUse()) return;
        List<Object> key = settingsKey(module);
        if (!key.equals(builtFor)) {
            close();
            builtFor = key;
            failed = false;
            chain = build(module);
        }
        if (chain != null) chain.process(main, allocator);
    }

    public void close() {
        if (chain != null) chain.close();
        chain = null;
        builtFor = null;
    }

    private static List<Object> settingsKey(PostEffectsModule m) {
        List<Object> key = new ArrayList<>();
        m.getSettings().forEach(s -> key.add(s.get()));
        return key;
    }

    private @Nullable PostChain build(PostEffectsModule m) {
        List<PostChainConfig.Pass> passes = new ArrayList<>();
        Identifier current = MAIN;

        if (m.motionBlur.get()) {
            passes.add(pass("motion_blur", List.of(input("In", current), input("Prev", PREVIOUS)), SWAP_A,
                    Map.of("MotionBlurConfig", List.of(f(m.motionBlurStrength.getFloat())))));
            passes.add(blit(SWAP_A, PREVIOUS));
            current = SWAP_A;
        }
        if (m.bloom.get()) {
            passes.add(pass("bloom_threshold", List.of(input("In", current)), BLOOM_A,
                    Map.of("BloomThresholdConfig", List.of(f(m.bloomThreshold.getFloat()), f(0.1f)))));
            float r = m.bloomRadius.getFloat();
            for (float radius : new float[] {r, r * BLOOM_SECOND_PASS}) {
                passes.add(blur(BLOOM_A, BLOOM_B, 1, 0, radius));
                passes.add(blur(BLOOM_B, BLOOM_A, 0, 1, radius));
            }
            Identifier out = current == SWAP_A ? SWAP_B : SWAP_A;
            passes.add(pass("bloom_combine", List.of(input("In", current), input("Bloom", BLOOM_A)), out,
                    Map.of("BloomCombineConfig", List.of(f(m.bloomIntensity.getFloat())))));
            current = out;
        }
        boolean grade = m.colorGrading.get() || m.vignette.get() || m.chromatic.get();
        if (grade) {
            int tint = m.tint.get();
            Identifier out = current == SWAP_A ? SWAP_B : SWAP_A;
            passes.add(pass("grade", List.of(input("In", current)), out, Map.of("GradeConfig", List.of(
                    f(m.colorGrading.get() ? m.saturation.getFloat() : 1f),
                    f(m.colorGrading.get() ? m.contrast.getFloat() : 1f),
                    f(m.vignette.get() ? m.vignetteStrength.getFloat() : 0f),
                    f(m.chromatic.get() ? m.chromaticAmount.getFloat() : 0f),
                    new UniformValue.Vec4Uniform(new Vector4f(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f,
                            m.colorGrading.get() ? m.tintStrength.getFloat() : 0f))))));
            current = out;
        }
        if (passes.isEmpty()) return null;
        if (current != MAIN) passes.add(blit(current, MAIN));

        Map<Identifier, PostChainConfig.InternalTarget> targets = new LinkedHashMap<>();
        for (Identifier id : new Identifier[] {SWAP_A, SWAP_B, BLOOM_A, BLOOM_B}) {
            targets.put(id, new PostChainConfig.InternalTarget(Optional.empty(), Optional.empty(), false, 0));
        }
        targets.put(PREVIOUS, new PostChainConfig.InternalTarget(Optional.empty(), Optional.empty(), true, 0));
        try {
            return PostChain.load(new PostChainConfig(targets, passes), Minecraft.getInstance().getTextureManager(),
                    Set.of(MAIN), ArchangelClient.id("post_effects"), projection);
        } catch (Exception e) {
            if (!failed) ArchangelClient.LOGGER.error("Could not build post effects", e);
            failed = true;
            return null;
        }
    }

    private static PostChainConfig.Pass pass(String shader, List<PostChainConfig.Input> inputs, Identifier out,
                                             Map<String, List<UniformValue>> uniforms) {
        return new PostChainConfig.Pass(SCREENQUAD, ArchangelClient.id("post/" + shader), inputs, out, uniforms);
    }

    private static PostChainConfig.Pass blur(Identifier in, Identifier out, float dx, float dy, float radius) {
        return pass("bloom_blur", List.of(input("In", in)), out,
                Map.of("BloomBlurConfig", List.of(new UniformValue.Vec2Uniform(new Vector2f(dx, dy)), f(radius))));
    }

    private static PostChainConfig.Pass blit(Identifier in, Identifier out) {
        return new PostChainConfig.Pass(SCREENQUAD, BLIT, List.of(input("In", in)), out,
                Map.of("BlitConfig", List.of(new UniformValue.Vec4Uniform(new Vector4f(1, 1, 1, 1)))));
    }

    private static PostChainConfig.Input input(String sampler, Identifier target) {
        return new PostChainConfig.TargetInput(sampler, target, false, true);
    }

    private static UniformValue f(float value) {
        return new UniformValue.FloatUniform(value);
    }
}

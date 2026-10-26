package dev.archangelclient.cosmetics.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import dev.archangelclient.ArchangelClient;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * Additive "light" render types for auras, trails and sparks. Built from vanilla shaders; they keep
 * the normal depth test (only depth writes are off), so effects are hidden behind blocks exactly
 * like the player they belong to.
 */
public final class CosmeticRenderTypes {
    private CosmeticRenderTypes() {}

    /** Untextured position+color quads, blended additively. */
    private static final RenderPipeline GLOW_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(ArchangelClient.id("pipeline/glow"))
                    .withVertexShader("core/rendertype_lightning")
                    .withFragmentShader("core/rendertype_lightning")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .build());

    /** Textured entity geometry (models), additive and full bright, with a scrollable texture. */
    private static final RenderPipeline SHELL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(ArchangelClient.id("pipeline/glow_shell"))
                    .withVertexShader("core/entity")
                    .withFragmentShader("core/entity")
                    .withShaderDefine("EMISSIVE")
                    .withShaderDefine("NO_OVERLAY")
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withShaderDefine("APPLY_TEXTURE_MATRIX")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
                    .build());

    /** Like vanilla "eyes" (full bright, translucent) but double-sided, for flat cosmetic planes such as wings. */
    private static final RenderPipeline EMISSIVE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(ArchangelClient.id("pipeline/emissive_cosmetic"))
                    .withVertexShader("core/entity")
                    .withFragmentShader("core/entity")
                    .withShaderDefine("EMISSIVE")
                    .withShaderDefine("NO_OVERLAY")
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
                    .build());

    private static final Function<Identifier, RenderType> EMISSIVE = Util.memoize(texture -> RenderType.create(
            "archangel_emissive", RenderSetup.builder(EMISSIVE_PIPELINE).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));

    public static RenderType emissive(Identifier texture) {
        return EMISSIVE.apply(texture);
    }

    public static final RenderType GLOW = RenderType.create("archangel_glow",
            RenderSetup.builder(GLOW_PIPELINE).sortOnUpload().createRenderSetup());

    /** Glowing copy of a model, tinted by the vertex color; the texture scrolls by (u, v). */
    public static RenderType shell(Identifier texture, float u, float v) {
        return RenderType.create("archangel_glow_shell",
                RenderSetup.builder(SHELL_PIPELINE)
                        .withTexture("Sampler0", texture, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                        .setTextureTransform(new TextureTransform.OffsetTextureTransform(u, v))
                        .useLightmap()
                        .useOverlay()
                        .sortOnUpload()
                        .createRenderSetup());
    }
}

package dev.archangelclient.sky;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.shader.IrisBridge;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * Draws the nebula cube map ({@code textures/environment/nebula.png}, made by
 * {@code tools/sky/nebula.py}) around the camera, after the sky color and before the sun, moon and
 * stars, so those still show on top. Called from {@code SkyRendererMixin}.
 */
public final class NebulaSky {
    private static final Identifier TEXTURE = ArchangelClient.id("textures/environment/nebula.png");

    /** Same pipeline as the vanilla End sky, but without face culling, since we sit inside the cube. */
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ArchangelClient.id("pipeline/nebula_sky"))
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .build());

    /** (center, right, down) of each face, in atlas order (3 columns, 2 rows). Must match nebula.py. */
    private static final int[][][] FACES = {
            {{1, 0, 0}, {0, 0, -1}, {0, -1, 0}},
            {{-1, 0, 0}, {0, 0, 1}, {0, -1, 0}},
            {{0, 1, 0}, {1, 0, 0}, {0, 0, 1}},
            {{0, -1, 0}, {1, 0, 0}, {0, 0, -1}},
            {{0, 0, 1}, {1, 0, 0}, {0, -1, 0}},
            {{0, 0, -1}, {-1, 0, 0}, {0, -1, 0}},
    };
    private static final int ATLAS_COLUMNS = 3;
    private static final int ATLAS_ROWS = 2;
    /** Pixels per face in the atlas; used to keep UVs half a texel inside each face (no bleeding). */
    private static final int FACE_SIZE = 512;
    /** Half size of the cube; inside the far plane like the vanilla sky geometry. */
    private static final float RADIUS = 100f;
    private static final int INDEX_COUNT = FACES.length * 6;
    /** Vanilla's star brightness on a clear night; the nebula reaches full opacity there. */
    private static final float FULL_NIGHT_STAR_BRIGHTNESS = 0.5f;

    private static @Nullable GpuBuffer cube;

    private NebulaSky() {}

    /**
     * @param starAngle       rotation of the vanilla star field, in radians
     * @param rainBrightness  1 when clear, 0 in full rain
     * @param starBrightness  vanilla star brightness (0 by day)
     */
    public static void render(PoseStack poseStack, float starAngle, float rainBrightness, float starBrightness) {
        NebulaSkyModule module = ArchangelClient.modules().get(NebulaSkyModule.class);
        if (!module.isEnabled() || IrisBridge.isShaderPackInUse()) return;
        float visibility = module.when.is(NebulaSkyModule.ALWAYS) ? 1f : Mth.clamp(starBrightness / FULL_NIGHT_STAR_BRIGHTNESS, 0f, 1f);
        float alpha = visibility * rainBrightness * module.opacity.getFloat();
        if (alpha <= 0f) return;

        Minecraft mc = Minecraft.getInstance();
        AbstractTexture texture = mc.getTextureManager().getTexture(TEXTURE);
        if (cube == null) cube = buildCube();

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90f));
        if (module.rotate.get()) poseStack.mulPose(Axis.XP.rotation(starAngle));
        modelView.mul(poseStack.last().pose());
        poseStack.popPose();

        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(INDEX_COUNT);
        GpuTextureView color = mc.getMainRenderTarget().getColorTextureView();
        GpuTextureView depth = mc.getMainRenderTarget().getDepthTextureView();
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(modelView, new Vector4f(1f, 1f, 1f, alpha), new Vector3f(), new Matrix4f());
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "Archangel nebula sky", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture("Sampler0", texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, cube);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.drawIndexed(0, 0, INDEX_COUNT, 1);
        }
        modelView.popMatrix();
    }

    private static GpuBuffer buildCube() {
        VertexFormat format = DefaultVertexFormat.POSITION_TEX_COLOR;
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(FACES.length * 4 * format.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, format);
            float inset = 0.5f / FACE_SIZE;
            for (int i = 0; i < FACES.length; i++) {
                float u0 = (i % ATLAS_COLUMNS) / (float) ATLAS_COLUMNS;
                float v0 = (i / ATLAS_COLUMNS) / (float) ATLAS_ROWS;
                corner(builder, FACES[i], -1, -1, u0, v0, inset);
                corner(builder, FACES[i], -1, 1, u0, v0, inset);
                corner(builder, FACES[i], 1, 1, u0, v0, inset);
                corner(builder, FACES[i], 1, -1, u0, v0, inset);
            }
            try (MeshData mesh = builder.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(() -> "Archangel nebula sky", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    /** One cube corner at face coordinates (su, sv) in {-1, 1}, with its atlas UV. */
    private static void corner(BufferBuilder builder, int[][] face, int su, int sv, float u0, float v0, float inset) {
        int[] c = face[0], r = face[1], d = face[2];
        float x = (c[0] + su * r[0] + sv * d[0]) * RADIUS;
        float y = (c[1] + su * r[1] + sv * d[1]) * RADIUS;
        float z = (c[2] + su * r[2] + sv * d[2]) * RADIUS;
        float faceU = su < 0 ? inset : 1f - inset;
        float faceV = sv < 0 ? inset : 1f - inset;
        builder.addVertex(x, y, z)
                .setUv(u0 + faceU / ATLAS_COLUMNS, v0 + faceV / ATLAS_ROWS)
                .setColor(-1);
    }
}

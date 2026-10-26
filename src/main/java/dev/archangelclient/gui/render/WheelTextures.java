package dev.archangelclient.gui.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.archangelclient.ArchangelClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Anti-aliased white shapes for radial menus, generated once per slot count: one ring wedge per
 * slot (each covering the whole wheel area, so they all draw into the same square) and a disc.
 * Tint them when drawing. Linear filtering keeps edges smooth at any GUI scale.
 */
public final class WheelTextures {
    public static final float INNER = 0.38f;
    private static final int SIZE = 512;
    /** Gap between wedges, in pixels of the texture at the outer edge. */
    private static final float GAP = 5f;

    private static int builtFor = -1;
    private static final List<Identifier> wedges = new ArrayList<>();
    private static Identifier disc;
    private static Identifier glow;

    private WheelTextures() {}

    /** Wedge {@code i} of {@code count}, clockwise from the top. */
    public static Identifier wedge(int count, int i) {
        ensure(count);
        return wedges.get(i);
    }

    public static Identifier disc() {
        if (disc == null) {
            disc = ArchangelClient.id("dynamic/wheel_disc");
            Minecraft.getInstance().getTextureManager().register(disc, texture("wheel disc", discImage()));
        }
        return disc;
    }

    /** A soft radial falloff, opaque in the middle and clear at the edge. */
    public static Identifier glow() {
        if (glow == null) {
            glow = ArchangelClient.id("dynamic/wheel_glow");
            Minecraft.getInstance().getTextureManager().register(glow, texture("wheel glow", glowImage()));
        }
        return glow;
    }

    private static void ensure(int count) {
        if (count == builtFor) return;
        var textures = Minecraft.getInstance().getTextureManager();
        for (Identifier id : wedges) textures.release(id);
        wedges.clear();
        for (int i = 0; i < count; i++) {
            Identifier id = ArchangelClient.id("dynamic/wheel_wedge_" + i);
            textures.register(id, texture("wheel wedge", wedgeImage(count, i)));
            wedges.add(id);
        }
        builtFor = count;
    }

    private static DynamicTexture texture(String label, NativeImage image) {
        return new DynamicTexture(() -> label, image) {
            {
                sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            }
        };
    }

    private static NativeImage wedgeImage(int count, int index) {
        NativeImage image = new NativeImage(SIZE, SIZE, true);
        float c = SIZE / 2f, outer = c - 2, inner = outer * INNER;
        double span = Math.PI * 2 / count, mid = index * span;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float dx = x + 0.5f - c, dy = y + 0.5f - c;
                float r = (float) Math.sqrt(dx * dx + dy * dy);
                // clockwise angle from straight up
                double a = Math.atan2(dx, -dy);
                double d = Math.IEEEremainder(a - mid, Math.PI * 2);
                // distance (px) from the wedge's side edges, with a constant-width gap
                float side = (float) (Math.sin(span / 2 - Math.abs(d)) * r) - GAP / 2;
                if (Math.abs(d) > span / 2) side = -1;
                float cover = clamp(side + 0.5f) * clamp(outer - r + 0.5f) * clamp(r - inner + 0.5f);
                image.setPixel(x, y, ((int) (cover * 255) << 24) | 0xFFFFFF);
            }
        }
        return image;
    }

    private static NativeImage discImage() {
        NativeImage image = new NativeImage(SIZE / 2, SIZE / 2, true);
        float c = SIZE / 4f, radius = c - 2;
        for (int y = 0; y < SIZE / 2; y++) {
            for (int x = 0; x < SIZE / 2; x++) {
                float dx = x + 0.5f - c, dy = y + 0.5f - c;
                float cover = clamp(radius - (float) Math.sqrt(dx * dx + dy * dy) + 0.5f);
                image.setPixel(x, y, ((int) (cover * 255) << 24) | 0xFFFFFF);
            }
        }
        return image;
    }

    private static NativeImage glowImage() {
        int size = SIZE / 4;
        NativeImage image = new NativeImage(size, size, true);
        float c = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x + 0.5f - c, dy = y + 0.5f - c;
                float t = clamp(1 - (float) Math.sqrt(dx * dx + dy * dy) / c);
                image.setPixel(x, y, ((int) (t * t * (3 - 2 * t) * 255) << 24) | 0xFFFFFF);
            }
        }
        return image;
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}

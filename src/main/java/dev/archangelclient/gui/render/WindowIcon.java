package dev.archangelclient.gui.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import dev.archangelclient.ArchangelClient;
import java.io.InputStream;
import java.nio.ByteBuffer;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Replaces the window icon with the Archangel wings (16 and 32 px). macOS ignores window icons. */
public final class WindowIcon {
    private static final String[] SIZES = {"icon_16.png", "icon_32.png"};

    private WindowIcon() {}

    public static void apply(Window window) {
        if (Util.getPlatform() == Util.OS.OSX) return;
        ByteBuffer[] pixels = new ByteBuffer[SIZES.length];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer images = GLFWImage.malloc(SIZES.length, stack);
            for (int i = 0; i < SIZES.length; i++) {
                String path = "/assets/" + ArchangelClient.MOD_ID + "/textures/gui/" + SIZES[i];
                try (InputStream in = WindowIcon.class.getResourceAsStream(path);
                     NativeImage image = NativeImage.read(in)) {
                    pixels[i] = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
                    // NativeImage stores ABGR, which matches GLFW's RGBA byte order on little-endian machines
                    pixels[i].asIntBuffer().put(image.getPixelsABGR());
                    images.position(i).width(image.getWidth()).height(image.getHeight()).pixels(pixels[i]);
                }
            }
            GLFW.glfwSetWindowIcon(window.handle(), images.position(0));
        } catch (Exception e) {
            ArchangelClient.LOGGER.warn("Could not set window icon", e);
        } finally {
            for (ByteBuffer b : pixels) if (b != null) MemoryUtil.memFree(b);
        }
    }
}

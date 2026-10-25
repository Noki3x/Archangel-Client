package dev.archangelclient.hud;

import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ColorSetting;
import dev.archangelclient.core.setting.NumberSetting;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A movable, scalable HUD element. Position is stored as a 0..1 fraction of the free space on each
 * axis (0 = left/top edge, 1 = right/bottom edge), so layouts survive window resizes. Position and
 * scale live in hidden settings, which puts them in the profile along with everything else.
 */
public abstract class HudElement extends Module {
    public static final int MARGIN = 4;
    public static final int LINE_HEIGHT = 15;

    private final NumberSetting posX;
    private final NumberSetting posY;
    private final NumberSetting scale;
    protected final BooleanSetting background;
    protected final ColorSetting textColor;

    /** Unscaled size from the last render; subclasses set it while rendering. */
    protected int width = 40;
    protected int height = LINE_HEIGHT;
    /** Screen position used for the current frame, for drawing that can't follow the pose (3D models). */
    protected int placedX;
    protected int placedY;

    protected HudElement(String name, String description, float defaultX, float defaultY, boolean enabledByDefault) {
        super(name, description, Category.HUD, enabledByDefault);
        posX = add(new NumberSetting("x", "", defaultX, 0, 1, 0).visibleWhen(() -> false));
        posY = add(new NumberSetting("y", "", defaultY, 0, 1, 0).visibleWhen(() -> false));
        scale = add(new NumberSetting("Scale", "Size of the element (scroll over it in the HUD editor)", 1, 0.5, 2, 0.05).suffix("x"));
        background = add(new BooleanSetting("Background", "Draw a translucent panel behind the element", true));
        textColor = add(new ColorSetting("Text color", "Color of the element's text", Theme.TEXT, false));
    }

    /** Draws the element with its top-left corner at (0, 0), unscaled, and updates width/height. */
    public abstract void render(GuiGraphics g, float partialTick, boolean editor);

    /** Whether to draw this frame. The HUD editor always shows enabled elements, as a preview. */
    public boolean shouldRender(boolean editor) {
        return true;
    }

    /** Draws at the stored position and scale. */
    public final void renderPlaced(GuiGraphics g, float partialTick, boolean editor, int screenW, int screenH) {
        placedX = screenX(screenW);
        placedY = screenY(screenH);
        g.pose().pushMatrix();
        g.pose().translate(placedX, placedY);
        g.pose().scale(getScale(), getScale());
        render(g, partialTick, editor);
        g.pose().popMatrix();
    }

    public float getScale() {
        return scale.getFloat();
    }

    public void setScale(double value) {
        scale.set(value);
    }

    public int scaledWidth() {
        return Math.round(width * getScale());
    }

    public int scaledHeight() {
        return Math.round(height * getScale());
    }

    public int screenX(int screenW) {
        return Math.round(MARGIN + posX.getFloat() * Math.max(0, screenW - 2 * MARGIN - scaledWidth()));
    }

    public int screenY(int screenH) {
        return Math.round(MARGIN + posY.getFloat() * Math.max(0, screenH - 2 * MARGIN - scaledHeight()));
    }

    public void setScreenPos(int x, int y, int screenW, int screenH) {
        int freeW = screenW - 2 * MARGIN - scaledWidth(), freeH = screenH - 2 * MARGIN - scaledHeight();
        posX.set(freeW <= 0 ? 0 : (x - MARGIN) / (double) freeW);
        posY.set(freeH <= 0 ? 0 : (y - MARGIN) / (double) freeH);
    }

    public void resetLayout() {
        posX.reset();
        posY.reset();
        scale.reset();
    }

    // --- drawing helpers for subclasses

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    protected void panel(GuiGraphics g, int w, int h) {
        if (background.get()) RenderUtil.roundedRect(g, 0, 0, w, h, Theme.RADIUS, Theme.PANEL);
    }

    protected void text(GuiGraphics g, String text, int x, int y) {
        Fonts.draw(g, text, x, y, textColor.get());
    }
}

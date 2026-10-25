package dev.archangelclient.gui.widget;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base for the client's own GUI widgets. The owning screen positions them and forwards input;
 * coordinates are GUI pixels. Kept independent of vanilla's widget classes so the settings GUI
 * doesn't break when Mojang reshuffles their input API.
 */
public abstract class UiWidget {
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    public UiWidget setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    /** Height this widget wants; may change when it expands (dropdowns, color pickers). */
    public int getHeight() {
        return height;
    }

    public boolean isVisible() {
        return true;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getHeight();
    }

    public abstract void render(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        return false;
    }

    public boolean charTyped(int codepoint) {
        return false;
    }
}

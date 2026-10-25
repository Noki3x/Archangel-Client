package dev.archangelclient.gui.widget;

import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Clipped, smoothly scrolling container. Children are positioned in content coordinates (0, 0 is
 * the panel's top-left at scroll 0); the panel translates rendering and mouse input.
 */
public class ScrollPanel extends UiWidget {
    private static final float SCROLL_STEP = 24f;
    private final List<UiWidget> children = new ArrayList<>();
    private float scroll;
    private float targetScroll;
    private boolean draggingBar;

    public List<UiWidget> children() {
        return children;
    }

    public void clear() {
        children.clear();
        scroll = targetScroll = 0;
    }

    public <W extends UiWidget> W add(W widget) {
        children.add(widget);
        return widget;
    }

    private int contentHeight() {
        int max = 0;
        for (UiWidget w : children) if (w.isVisible()) max = Math.max(max, w.getY() + w.getHeight());
        return max + 4;
    }

    private float maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        targetScroll = Math.max(0, Math.min(maxScroll(), targetScroll));
        scroll += (targetScroll - scroll) * 0.35f;
        if (Math.abs(targetScroll - scroll) < 0.25f) scroll = targetScroll;

        boolean inside = isHovered(mouseX, mouseY);
        int lx = inside ? mouseX - x : Integer.MIN_VALUE / 2;
        int ly = inside ? Math.round(mouseY - y + scroll) : Integer.MIN_VALUE / 2;
        g.enableScissor(x, y, x + width, y + height);
        g.pose().pushMatrix();
        g.pose().translate(x, y - scroll);
        for (UiWidget w : children) {
            if (!w.isVisible()) continue;
            if (w.getY() + w.getHeight() < scroll || w.getY() > scroll + height) continue;
            w.render(g, lx, ly, partialTick);
        }
        g.pose().popMatrix();
        g.disableScissor();

        float max = maxScroll();
        if (max > 0) {
            int barH = Math.max(16, Math.round(height * height / (float) contentHeight()));
            int barY = y + Math.round((height - barH) * (scroll / max));
            RenderUtil.roundedRect(g, x + width + 3, barY, 2, barH, 1, Colors.withAlpha(Theme.TEXT, draggingBar ? 0x90 : 0x40));
        }
    }

    public void scrollBy(double amount) {
        targetScroll -= (float) amount * SCROLL_STEP;
    }

    public void scrollToTop() {
        scroll = targetScroll = 0;
    }

    private double localX(double mouseX) {
        return mouseX - x;
    }

    private double localY(double mouseY) {
        return mouseY - y + scroll;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (maxScroll() > 0 && mouseX >= x + width && mouseX < x + width + 8 && mouseY >= y && mouseY < y + height) {
            draggingBar = true;
            return true;
        }
        if (!isHovered(mouseX, mouseY)) {
            // still let children see outside clicks, so open fields and dropdowns can close
            for (UiWidget w : children) if (w.isVisible()) w.mouseClicked(Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, button);
            return false;
        }
        boolean handled = false;
        for (UiWidget w : List.copyOf(children)) {
            if (!w.isVisible()) continue;
            if (!handled && w.mouseClicked(localX(mouseX), localY(mouseY), button)) handled = true;
            else if (handled) w.mouseClicked(Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, button);
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (draggingBar) {
            float max = maxScroll();
            targetScroll = scroll = (float) Math.max(0, Math.min(max, (mouseY - y) / height * contentHeight() - height / 2f));
            return true;
        }
        for (UiWidget w : children) if (w.isVisible() && w.mouseDragged(localX(mouseX), localY(mouseY), button)) return true;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        boolean handled = false;
        for (UiWidget w : children) handled |= w.mouseReleased(localX(mouseX), localY(mouseY), button);
        return handled;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        for (UiWidget w : children) if (w.isVisible() && w.keyPressed(key, scancode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean charTyped(int codepoint) {
        for (UiWidget w : children) if (w.isVisible() && w.charTyped(codepoint)) return true;
        return false;
    }
}

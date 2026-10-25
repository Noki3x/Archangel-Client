package dev.archangelclient.gui.screen;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.MenuBackground;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.gui.widget.TextButton;
import dev.archangelclient.gui.widget.UiWidget;
import dev.archangelclient.hud.HudElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Drag-to-position HUD editor: a faint grid, snapping to screen edges, the center lines and other
 * elements (with guide lines while snapped), scroll to resize, right-click for an element's
 * settings, arrow keys to nudge, and a reset button.
 */
public class HudEditorScreen extends FadingScreen {
    private static final int GRID = 10;
    private static final int SNAP = 4;

    private final Screen parent;
    private final List<UiWidget> buttons = new ArrayList<>();
    private HudElement dragging;
    private HudElement selected;
    private double dragOffsetX, dragOffsetY;
    private Integer guideX, guideY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    private List<HudElement> elements() {
        return ArchangelClient.hud().elements().stream().filter(HudElement::isEnabled).toList();
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        int bw = 70, gap = 6, total = 3 * bw + 2 * gap;
        int bx = (width - total) / 2, by = height / 2 + 14;
        buttons.add(new TextButton("Reset layout", TextButton.Style.DANGER, () -> ArchangelClient.hud().resetLayout())
                .setBounds(bx, by, bw, 16));
        buttons.add(new TextButton("Settings", TextButton.Style.NORMAL, () -> minecraft.setScreen(new ClientSettingsScreen(this)))
                .setBounds(bx + bw + gap, by, bw, 16));
        buttons.add(new TextButton("Done", TextButton.Style.ACCENT, this::onClose).setBounds(bx + 2 * (bw + gap), by, bw, 16));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // keep the world visible (no blur) so the layout can be judged against the real view
        if (minecraft.level == null) MenuBackground.render(g, width, height, 1f);
        g.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float a = fade();
        renderGrid(g, a);

        HudElement hovered = elementAt(mouseX, mouseY);
        for (HudElement e : elements()) {
            if (!e.shouldRender(true)) continue;
            e.renderPlaced(g, partialTick, true, width, height);
            int x = e.screenX(width), y = e.screenY(height), w = e.scaledWidth(), h = e.scaledHeight();
            boolean active = e == dragging || e == hovered || e == selected;
            outline(g, x - 1, y - 1, w + 2, h + 2, active ? Theme.ACCENT_LIGHT : Colors.withAlpha(Theme.TEXT, 0x40));
            if (e == hovered && dragging == null) {
                String label = e.getName() + "  " + Math.round(e.getScale() * 100) + "%";
                int ly = y - 11 < 0 ? y + h + 3 : y - 11;
                Fonts.draw(g, label, x, ly, Theme.ACCENT_LIGHT);
            }
        }
        if (guideX != null) g.fill(guideX, 0, guideX + 1, height, Theme.ACCENT_PURPLE);
        if (guideY != null) g.fill(0, guideY, width, guideY + 1, Theme.ACCENT_PURPLE);

        // center help card and buttons
        if (dragging == null) {
            String hint = "Drag to move · Scroll to resize · Right-click for options";
            int cw = Math.max(236, Fonts.width(hint) + 24), ch = 50, cx = (width - cw) / 2, cy = height / 2 - 26;
            RenderUtil.roundedRect(g, cx, cy, cw, ch, Theme.RADIUS_LARGE, Colors.fade(Theme.PANEL_SOLID, a));
            Fonts.drawCentered(g, "Edit HUD Layout", width / 2, cy + 8, Colors.fade(Theme.TEXT, a));
            Fonts.drawCentered(g, hint, width / 2, cy + 22,
                    Colors.fade(Theme.TEXT_MUTED, a));
            for (UiWidget b : buttons) b.render(g, mouseX, mouseY, partialTick);
        }
    }

    private void renderGrid(GuiGraphics g, float alpha) {
        int line = Colors.fade(0x0EFFFFFF, alpha);
        for (int x = GRID; x < width; x += GRID) g.fill(x, 0, x + 1, height, line);
        for (int y = GRID; y < height; y += GRID) g.fill(0, y, width, y + 1, line);
        int center = Colors.fade(Colors.withAlpha(Theme.ACCENT, 0x30), alpha);
        g.fill(width / 2, 0, width / 2 + 1, height, center);
        g.fill(0, height / 2, width, height / 2 + 1, center);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Topmost element under the mouse (later elements draw on top). */
    private HudElement elementAt(double mx, double my) {
        List<HudElement> list = elements();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudElement e = list.get(i);
            int x = e.screenX(width), y = e.screenY(height);
            if (mx >= x && mx < x + e.scaledWidth() && my >= y && my < y + e.scaledHeight()) return e;
        }
        return null;
    }

    // --- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        for (UiWidget b : buttons) if (b.mouseClicked(mx, my, event.button())) return true;
        HudElement e = elementAt(mx, my);
        selected = e;
        if (e == null) return super.mouseClicked(event, doubleClick);
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            minecraft.setScreen(new ClientSettingsScreen(this, e));
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = e;
            dragOffsetX = mx - e.screenX(width);
            dragOffsetY = my - e.screenY(height);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging == null) return super.mouseDragged(event, dragX, dragY);
        int w = dragging.scaledWidth(), h = dragging.scaledHeight();
        int x = (int) Math.round(event.x() - dragOffsetX), y = (int) Math.round(event.y() - dragOffsetY);
        guideX = guideY = null;
        if ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) == 0) {
            // snap the element's left, center or right edge to the screen and to other elements
            List<Integer> xs = new ArrayList<>(List.of(HudElement.MARGIN, width / 2, width - HudElement.MARGIN));
            List<Integer> ys = new ArrayList<>(List.of(HudElement.MARGIN, height / 2, height - HudElement.MARGIN));
            for (HudElement o : elements()) {
                if (o == dragging) continue;
                int ox = o.screenX(width), oy = o.screenY(height);
                xs.addAll(List.of(ox, ox + o.scaledWidth() / 2, ox + o.scaledWidth()));
                ys.addAll(List.of(oy, oy + o.scaledHeight() / 2, oy + o.scaledHeight()));
            }
            int[] sx = snap(x, w, xs);
            int[] sy = snap(y, h, ys);
            x = sx[0];
            y = sy[0];
            guideX = sx[1] == Integer.MIN_VALUE ? null : sx[1];
            guideY = sy[1] == Integer.MIN_VALUE ? null : sy[1];
        }
        x = Math.max(HudElement.MARGIN, Math.min(width - HudElement.MARGIN - w, x));
        y = Math.max(HudElement.MARGIN, Math.min(height - HudElement.MARGIN - h, y));
        dragging.setScreenPos(x, y, width, height);
        return true;
    }

    /** Returns {snapped position, guide line or MIN_VALUE} for an element at {@code pos} of size {@code size}. */
    private static int[] snap(int pos, int size, List<Integer> targets) {
        int[] edges = {pos, pos + size / 2, pos + size};
        int best = SNAP + 1, result = pos, guide = Integer.MIN_VALUE;
        for (int i = 0; i < edges.length; i++) {
            for (int t : targets) {
                int d = Math.abs(edges[i] - t);
                if (d < best) {
                    best = d;
                    result = pos + (t - edges[i]);
                    guide = t;
                }
            }
        }
        return new int[] {result, guide};
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        guideX = guideY = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        HudElement e = elementAt(mouseX, mouseY);
        if (e == null) return false;
        e.setScale(e.getScale() + Math.signum(scrollY) * 0.05);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (selected != null) {
            int dx = 0, dy = 0;
            switch (event.key()) {
                case GLFW.GLFW_KEY_LEFT -> dx = -1;
                case GLFW.GLFW_KEY_RIGHT -> dx = 1;
                case GLFW.GLFW_KEY_UP -> dy = -1;
                case GLFW.GLFW_KEY_DOWN -> dy = 1;
                default -> {}
            }
            if (dx != 0 || dy != 0) {
                selected.setScreenPos(selected.screenX(width) + dx, selected.screenY(height) + dy, width, height);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ArchangelClient.config().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

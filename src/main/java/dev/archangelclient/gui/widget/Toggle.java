package dev.archangelclient.gui.widget;

import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Theme;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;

/** Pill-shaped on/off switch with a sliding knob. */
public class Toggle extends UiWidget {
    public static final int WIDTH = 22;
    public static final int HEIGHT = 12;

    private final BooleanSupplier state;
    private final Runnable onToggle;
    private final Animation knob;

    public Toggle(BooleanSupplier state, Runnable onToggle) {
        this.state = state;
        this.onToggle = onToggle;
        this.knob = new Animation(Theme.HOVER_MS, state.getAsBoolean() ? 1f : 0f);
        this.width = WIDTH;
        this.height = HEIGHT;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        knob.target(state.getAsBoolean() ? 1f : 0f);
        float k = knob.get();
        int track = Colors.lerp(Theme.TOGGLE_OFF, Theme.ACCENT, k);
        if (k > 0.05f) RenderUtil.glow(g, x, y, WIDTH, HEIGHT, HEIGHT / 2, Theme.ACCENT, k * 0.8f);
        RenderUtil.roundedRect(g, x, y, WIDTH, HEIGHT, HEIGHT / 2, track);
        int size = HEIGHT - 4;
        int kx = Math.round(x + 2 + (WIDTH - size - 4) * k);
        RenderUtil.roundedRect(g, kx, y + 2, size, size, size / 2, isHovered(mouseX, mouseY) ? 0xFFFFFFFF : 0xFFE5E7EB);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        onToggle.run();
        return true;
    }
}

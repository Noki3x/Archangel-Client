package dev.archangelclient.gui.widget;

import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;

/** Small rounded button with a label, used inside panels. */
public class TextButton extends UiWidget {
    public enum Style { NORMAL, ACCENT, DANGER }

    private final Supplier<String> label;
    private final Runnable onPress;
    private final Style style;
    private final Animation hover = new Animation(Theme.HOVER_MS, 0f);

    public TextButton(String label, Style style, Runnable onPress) {
        this(() -> label, style, onPress);
    }

    public TextButton(Supplier<String> label, Style style, Runnable onPress) {
        this.label = label;
        this.style = style;
        this.onPress = onPress;
        this.height = 16;
    }

    /** Width that fits the label with padding. */
    public int preferredWidth() {
        return Fonts.width(label.get()) + 16;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hover.target(isHovered(mouseX, mouseY) ? 1f : 0f);
        float h = hover.get();
        int base = switch (style) {
            case NORMAL -> Theme.FIELD;
            case ACCENT -> Colors.withAlpha(Theme.ACCENT, 0xC0);
            case DANGER -> Colors.withAlpha(Theme.DANGER, 0x40);
        };
        int hovered = switch (style) {
            case NORMAL -> Theme.HOVER_BAR;
            case ACCENT -> Theme.ACCENT;
            case DANGER -> Colors.withAlpha(Theme.DANGER, 0x80);
        };
        if (style == Style.ACCENT && h > 0.05f) RenderUtil.glow(g, x, y, width, height, Theme.RADIUS, Theme.ACCENT, h);
        RenderUtil.roundedRect(g, x, y, width, height, Theme.RADIUS, Colors.lerp(base, hovered, h));
        String text = Fonts.trim(label.get(), width - 8);
        Fonts.drawCentered(g, text, x + width / 2, y + (height - 8) / 2, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        onPress.run();
        return true;
    }
}

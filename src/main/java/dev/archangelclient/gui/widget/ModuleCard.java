package dev.archangelclient.gui.widget;

import dev.archangelclient.core.module.Module;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

/** Grid card for one module: name, description and a toggle. Clicking the card opens its settings. */
public class ModuleCard extends UiWidget {
    private final Module module;
    private final Consumer<Module> onOpen;
    private final Toggle toggle;
    private final Animation hover = new Animation(Theme.HOVER_MS, 0f);
    private final Animation enabled;

    public ModuleCard(Module module, Consumer<Module> onOpen) {
        this.module = module;
        this.onOpen = onOpen;
        this.toggle = new Toggle(module::isEnabled, module::toggle);
        this.enabled = new Animation(Theme.HOVER_MS, module.isEnabled() ? 1f : 0f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hover.target(isHovered(mouseX, mouseY) ? 1f : 0f);
        enabled.target(module.isEnabled() ? 1f : 0f);
        float h = hover.get(), e = enabled.get();

        if (e > 0.02f) RenderUtil.glow(g, x, y, width, height, Theme.RADIUS_LARGE, Theme.ACCENT, e * 0.35f);
        RenderUtil.roundedRect(g, x, y, width, height, Theme.RADIUS_LARGE, Colors.lerp(Theme.CARD, Theme.CARD_HOVER, h));
        // accent strip on the left edge of enabled modules
        if (e > 0.02f) RenderUtil.roundedRect(g, x, y + 8, 2, height - 16, 1, Colors.fade(Theme.ACCENT_LIGHT, e));

        Fonts.drawBold(g, Fonts.trim(module.getName(), width - 16), x + 9, y + 8, Theme.TEXT);
        List<FormattedCharSequence> lines = Fonts.wrap(module.getDescription(), width - 18);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.drawString(Fonts.font(), lines.get(i), x + 9, y + 20 + i * 10, Theme.TEXT_MUTED, false);
        }

        int ty = y + height - Toggle.HEIGHT - 7;
        toggle.setBounds(x + width - Toggle.WIDTH - 8, ty, Toggle.WIDTH, Toggle.HEIGHT);
        toggle.render(g, mouseX, mouseY, partialTick);
        if (!module.getSettings().isEmpty() || h > 0.02f) {
            Fonts.draw(g, "Options", x + 9, ty + 2, Colors.lerp(Theme.TEXT_DIM, Theme.ACCENT_LIGHT, h));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        if (!toggle.mouseClicked(mouseX, mouseY, button)) onOpen.accept(module);
        return true;
    }
}

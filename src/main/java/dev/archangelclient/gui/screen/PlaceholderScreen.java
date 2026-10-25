package dev.archangelclient.gui.screen;

import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.MenuBackground;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.gui.widget.RoundedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Temporary target for menu entries whose screens arrive in later milestones. */
public class PlaceholderScreen extends FadingScreen {
    private final Screen parent;

    public PlaceholderScreen(Screen parent, String name) {
        super(Component.literal(name));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new RoundedButton(width / 2 - 50, height / 2 + 20, 100, Theme.MENU_BUTTON_HEIGHT,
                Component.literal("Back"), b -> onClose()));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        MenuBackground.render(g, width, height, 1f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float a = fade();
        g.drawCenteredString(font, Fonts.bold(getTitle().getString()), width / 2, height / 2 - 14, Colors.fade(Theme.TEXT, a));
        g.drawCenteredString(font, Fonts.regular("Coming in a later milestone"), width / 2, height / 2, Colors.fade(Theme.TEXT_MUTED, a));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}

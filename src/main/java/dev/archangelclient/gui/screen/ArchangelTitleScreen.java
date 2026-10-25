package dev.archangelclient.gui.screen;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.MenuBackground;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.gui.widget.PlayerPreview;
import dev.archangelclient.gui.widget.RoundedButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Replacement main menu (reference photo 1): wings logo and "Archangel Client" above a column of
 * minimal text buttons, a 3D preview of the player's skin on the right, and an animated backdrop.
 */
public class ArchangelTitleScreen extends FadingScreen {
    private static final Identifier LOGO = ArchangelClient.id("textures/gui/logo.png");
    private static final int LOGO_TEXTURE_SIZE = 1024;

    private final List<RoundedButton> menuButtons = new ArrayList<>();
    private PlayerSkinWidget preview;
    private int logoSize = Theme.LOGO_SIZE;

    public ArchangelTitleScreen() {
        super(Component.literal(ArchangelClient.NAME));
    }

    @Override
    protected void init() {
        super.init();
        menuButtons.clear();
        // Shrink the logo on short windows so the column never runs into the version text
        logoSize = Math.max(Theme.LOGO_MIN_SIZE, Math.min(Theme.LOGO_SIZE, height - menuHeightWithoutLogo() - 40));

        int cx = (int) (width * Theme.MENU_COLUMN_X);
        int bx = cx - Theme.MENU_BUTTON_WIDTH / 2;
        int y = menuTop() + logoSize + Theme.TITLE_GAP + font.lineHeight + Theme.MENU_GROUP_GAP * 2;

        y = addMenuButton(bx, y, "singleplayer", () -> minecraft.setScreen(new SelectWorldScreen(this)));
        y = addMenuButton(bx, y, "multiplayer", () -> minecraft.setScreen(new JoinMultiplayerScreen(this)));
        y += Theme.MENU_GROUP_GAP;
        y = addMenuButton(bx, y, "settings", () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
        y = addMenuButton(bx, y, "client", () -> minecraft.setScreen(new PlaceholderScreen(this, "Client settings")));
        y += Theme.MENU_GROUP_GAP;
        addMenuButton(bx, y, "quit", minecraft::stop);

        // Right column: live skin preview
        int px = (int) (width * Theme.PREVIEW_COLUMN_X) - Theme.PREVIEW_WIDTH / 2;
        int py = (height - Theme.PREVIEW_HEIGHT) / 2 + font.lineHeight;
        preview = PlayerPreview.create(Theme.PREVIEW_WIDTH, Theme.PREVIEW_HEIGHT);
        preview.setPosition(px, py);
        addRenderableWidget(preview);

        // Bottom-right: cosmetics / account
        Component cosmetics = Component.translatable("archangelclient.menu.cosmetics");
        int cw = font.width(Fonts.regular(cosmetics)) + 16;
        var cosmeticsButton = new RoundedButton(width - cw - Theme.CORNER_MARGIN,
                height - Theme.MENU_BUTTON_HEIGHT - Theme.CORNER_MARGIN, cw, Theme.MENU_BUTTON_HEIGHT, cosmetics,
                b -> minecraft.setScreen(new PlaceholderScreen(this, "Cosmetics")));
        menuButtons.add(cosmeticsButton);
        addRenderableWidget(cosmeticsButton);
    }

    private int addMenuButton(int x, int y, String key, Runnable action) {
        var button = new RoundedButton(x, y, Theme.MENU_BUTTON_WIDTH, Theme.MENU_BUTTON_HEIGHT,
                Component.translatable("archangelclient.menu." + key), b -> action.run());
        menuButtons.add(button);
        addRenderableWidget(button);
        return y + Theme.MENU_BUTTON_HEIGHT + Theme.MENU_BUTTON_GAP;
    }

    /** Top of the logo, chosen so logo + title + buttons sit vertically centered. */
    private int menuTop() {
        return (height - logoSize - menuHeightWithoutLogo()) / 2;
    }

    private int menuHeightWithoutLogo() {
        int buttons = 5 * (Theme.MENU_BUTTON_HEIGHT + Theme.MENU_BUTTON_GAP) + 2 * Theme.MENU_GROUP_GAP;
        return Theme.TITLE_GAP + font.lineHeight + Theme.MENU_GROUP_GAP * 2 + buttons;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        MenuBackground.render(g, width, height, 1f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float alpha = fade();
        menuButtons.forEach(b -> b.setAlpha(alpha));
        super.render(g, mouseX, mouseY, partialTick); // background + widgets

        // Logo and title
        int cx = (int) (width * Theme.MENU_COLUMN_X);
        int top = menuTop();
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, cx - logoSize / 2, top, 0, 0,
                logoSize, logoSize, LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE,
                LOGO_TEXTURE_SIZE, Colors.fade(0xFFFFFFFF, alpha));
        g.drawCenteredString(font, Fonts.bold(ArchangelClient.NAME), cx, top + logoSize + Theme.TITLE_GAP,
                Colors.fade(Theme.TEXT, alpha));

        // Username above the preview, with a glowing underline
        int px = (int) (width * Theme.PREVIEW_COLUMN_X);
        int nameY = preview.getY() - font.lineHeight - 8;
        g.drawCenteredString(font, Fonts.bold(minecraft.getUser().getName()), px, nameY, Colors.fade(Theme.TEXT, alpha));
        RenderUtil.underline(g, px, nameY + font.lineHeight + 3, 14, Colors.fade(Theme.ACCENT_LIGHT, alpha));

        // Bottom-left: version
        String version = ArchangelClient.NAME + " " + modVersion() + "  ·  Minecraft " + SharedConstants.getCurrentVersion().name();
        g.drawString(font, Fonts.regular(version), Theme.CORNER_MARGIN,
                height - font.lineHeight - Theme.CORNER_MARGIN, Colors.fade(Theme.TEXT_DIM, alpha), false);
    }

    private static String modVersion() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(ArchangelClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

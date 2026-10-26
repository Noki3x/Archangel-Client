package dev.archangelclient.gui.screen;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.MenuBackground;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Easing;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.gui.widget.CosmeticPreview;
import dev.archangelclient.gui.widget.RoundedButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * Replacement main menu (reference photo 1): wings logo and "Archangel Client" above a column of
 * minimal text buttons, a 3D preview of the player wearing their cosmetics on the right, and an
 * animated backdrop.
 *
 * <p>The first time the menu opens in a session it plays an intro: the wings logo appears large in
 * the middle, then shrinks into its place as the menu items fade in one by one, and finally the
 * player walks in from the right with their name above their head. Any click or key skips it.
 */
public class ArchangelTitleScreen extends FadingScreen {
    private static final Identifier LOGO = ArchangelClient.id("textures/gui/logo.png");
    private static final int LOGO_TEXTURE_SIZE = 1024;

    /** Intro timeline marks, in ms from the start of the intro. */
    private static final long LOGO_MOVE_START = Theme.INTRO_LOGO_IN_MS + Theme.INTRO_LOGO_HOLD_MS;
    private static final long LOGO_SETTLED = LOGO_MOVE_START + Theme.INTRO_LOGO_MOVE_MS;
    private static final long WALK_START = LOGO_SETTLED + Theme.INTRO_WALK_DELAY_MS;
    private static final long WALK_END = WALK_START + Theme.INTRO_WALK_MS;
    private static final long INTRO_END = WALK_END + Theme.INTRO_TURN_MS;

    /** The intro plays once per game session. */
    private static boolean introPlayed;

    private final List<RoundedButton> menuButtons = new ArrayList<>();
    /** Each button's resting y, so the intro can slide them up into place. */
    private final List<Integer> buttonRestY = new ArrayList<>();
    private CosmeticPreview preview;
    private int logoSize = Theme.LOGO_SIZE;
    /** When this screen's intro started (0: not started), and whether it has one at all. */
    private long introStart;
    private final boolean hasIntro;

    public ArchangelTitleScreen() {
        super(Component.literal(ArchangelClient.NAME));
        hasIntro = !introPlayed;
        introPlayed = true;
    }

    @Override
    protected void init() {
        super.init();
        menuButtons.clear();
        buttonRestY.clear();
        // Shrink the logo on short windows so the column never runs into the version text
        logoSize = Math.max(Theme.LOGO_MIN_SIZE, Math.min(Theme.LOGO_SIZE, height - menuHeightWithoutLogo() - 40));

        int cx = (int) (width * Theme.MENU_COLUMN_X);
        int bx = cx - Theme.MENU_BUTTON_WIDTH / 2;
        int y = menuTop() + logoSize + Theme.TITLE_GAP + font.lineHeight + Theme.MENU_GROUP_GAP * 2;

        y = addMenuButton(bx, y, "singleplayer", () -> minecraft.setScreen(new SelectWorldScreen(this)));
        y = addMenuButton(bx, y, "multiplayer", () -> minecraft.setScreen(new JoinMultiplayerScreen(this)));
        y += Theme.MENU_GROUP_GAP;
        y = addMenuButton(bx, y, "settings", () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
        y = addMenuButton(bx, y, "client", () -> minecraft.setScreen(new ClientSettingsScreen(this)));
        y += Theme.MENU_GROUP_GAP;
        addMenuButton(bx, y, "quit", minecraft::stop);

        // Right column: live preview of the player and their equipped cosmetics
        int px = (int) (width * Theme.PREVIEW_COLUMN_X) - Theme.PREVIEW_WIDTH / 2;
        int py = (height - Theme.PREVIEW_HEIGHT) / 2 + font.lineHeight;
        preview = new CosmeticPreview(false);
        preview.setBounds(px, py, Theme.PREVIEW_WIDTH, Theme.PREVIEW_HEIGHT);

        // Bottom-right: cosmetics / account
        Component cosmetics = Component.translatable("archangelclient.menu.cosmetics");
        int cw = font.width(Fonts.regular(cosmetics)) + 16;
        var cosmeticsButton = new RoundedButton(width - cw - Theme.CORNER_MARGIN,
                height - Theme.MENU_BUTTON_HEIGHT - Theme.CORNER_MARGIN, cw, Theme.MENU_BUTTON_HEIGHT, cosmetics,
                b -> minecraft.setScreen(new CosmeticsScreen(this)));
        menuButtons.add(cosmeticsButton);
        buttonRestY.add(cosmeticsButton.getY());
        addRenderableWidget(cosmeticsButton);
    }

    private int addMenuButton(int x, int y, String key, Runnable action) {
        var button = new RoundedButton(x, y, Theme.MENU_BUTTON_WIDTH, Theme.MENU_BUTTON_HEIGHT,
                Component.translatable("archangelclient.menu." + key), b -> action.run());
        menuButtons.add(button);
        buttonRestY.add(y);
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

    /**
     * Milliseconds into the intro, or a value past its end when there is none (or it was skipped).
     * The intro waits for the resource loading overlay to clear so it isn't hidden behind it.
     */
    private long introTime() {
        if (!hasIntro) return INTRO_END;
        if (introStart == 0) {
            if (minecraft.getOverlay() != null) return 0;
            introStart = Util.getMillis();
        }
        return Util.getMillis() - introStart;
    }

    private void skipIntro() {
        if (hasIntro && introTime() < INTRO_END) introStart = Util.getMillis() - INTRO_END;
    }

    /** Opacity of the {@code index}th menu item, which fade in one after another during the intro. */
    private static float itemAlpha(long t, int index) {
        long start = LOGO_MOVE_START + Theme.INTRO_ITEMS_DELAY_MS + index * Theme.INTRO_ITEM_STAGGER_MS;
        return Easing.outCubic((t - start) / (float) Theme.INTRO_ITEM_FADE_MS);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float alpha = fade();
        long t = introTime();
        // item 0 is the title; buttons follow in order, the corner button last
        for (int i = 0; i < menuButtons.size(); i++) {
            RoundedButton button = menuButtons.get(i);
            float a = itemAlpha(t, i + 1);
            button.setAlpha(a * alpha);
            button.active = a > 0.5f;
            button.setY(buttonRestY.get(i) + Math.round((1 - a) * Theme.INTRO_ITEM_RISE));
        }
        super.render(g, mouseX, mouseY, partialTick); // background + widgets
        renderPlayer(g, mouseX, mouseY, partialTick, t, alpha);
        renderLogo(g, t, alpha);

        // Bottom-left: version, with the last batch of menu items
        float cornerAlpha = alpha * itemAlpha(t, menuButtons.size());
        String version = ArchangelClient.NAME + " " + modVersion() + "  ·  Minecraft " + SharedConstants.getCurrentVersion().name();
        g.drawString(font, Fonts.regular(version), Theme.CORNER_MARGIN,
                height - font.lineHeight - Theme.CORNER_MARGIN, Colors.fade(Theme.TEXT_DIM, cornerAlpha), false);
    }

    /** The wings logo: big and centered at the start of the intro, then gliding to its menu spot. */
    private void renderLogo(GuiGraphics g, long t, float alpha) {
        int cx = (int) (width * Theme.MENU_COLUMN_X);
        int top = menuTop();
        float appear = Easing.outCubic(t / (float) Theme.INTRO_LOGO_IN_MS);
        float move = Easing.inOutCubic((t - LOGO_MOVE_START) / (float) Theme.INTRO_LOGO_MOVE_MS);
        float bigSize = Math.min(width, height) * Theme.INTRO_LOGO_FRACTION
                * Easing.lerp(Theme.INTRO_LOGO_START_SCALE, 1f, appear);
        float size = Easing.lerp(bigSize, logoSize, move);
        float centerX = Easing.lerp(width / 2f, cx, move);
        float centerY = Easing.lerp(height / 2f, top + logoSize / 2f, move);
        float logoAlpha = alpha * appear;

        // a soft halo that fades as the logo settles
        float halo = logoAlpha * (1 - move);
        if (halo > 0.01f) {
            RenderUtil.radialGlow(g, Math.round(centerX), Math.round(centerY), Math.round(size * Theme.INTRO_LOGO_GLOW_RADIUS),
                    Theme.INTRO_LOGO_GLOW, halo);
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(centerX - size / 2f, centerY - size / 2f);
        pose.scale(size / logoSize, size / logoSize);
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0, 0, logoSize, logoSize, LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE,
                LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE, Colors.fade(0xFFFFFFFF, logoAlpha));
        pose.popMatrix();

        float titleAlpha = alpha * itemAlpha(t, 0);
        int titleY = top + logoSize + Theme.TITLE_GAP + Math.round((1 - itemAlpha(t, 0)) * Theme.INTRO_ITEM_RISE);
        g.drawCenteredString(font, Fonts.bold(ArchangelClient.NAME), cx, titleY, Colors.fade(Theme.TEXT, titleAlpha));
    }

    /**
     * The player preview. During the intro the player walks in from beyond the right edge, then turns
     * to face the viewer; the name and its underline ride along above the head the whole way.
     */
    private void renderPlayer(GuiGraphics g, int mouseX, int mouseY, float partialTick, long t, float alpha) {
        if (t < WALK_START) return;
        float ppb = preview.pixelsPerBlock();
        float distance = width + Theme.PREVIEW_WIDTH / 2f - (preview.getX() + preview.getWidth() / 2f); // GUI px
        float walked = Easing.clamp01((t - WALK_START) / (float) Theme.INTRO_WALK_MS);
        float walkedPx = distance * walked;
        int offsetX = Math.round(distance - walkedPx);
        if (t < INTRO_END) {
            float turn = Easing.inOutSine((t - WALK_END) / (float) Theme.INTRO_TURN_MS);
            // ease out of the stride as the player stops and turns
            float speed = Theme.INTRO_WALK_SPEED * (1 - turn);
            preview.setScript(new CosmeticPreview.Script(Easing.lerp(Theme.INTRO_WALK_YAW, Theme.PREVIEW_REST_YAW, turn),
                    walkedPx / ppb * Theme.INTRO_WALK_CYCLE_PER_BLOCK, speed));
        } else {
            preview.setScript(null);
        }
        preview.setRenderOffset(offsetX, 0);
        preview.setLoadout(ArchangelClient.cosmetics().localLoadout());
        preview.render(g, mouseX, mouseY, partialTick);

        // Username above the head, with a glowing underline
        int px = (int) (width * Theme.PREVIEW_COLUMN_X) + offsetX;
        int nameY = preview.getY() - font.lineHeight - 8;
        g.drawCenteredString(font, Fonts.bold(minecraft.getUser().getName()), px, nameY, Colors.fade(Theme.TEXT, alpha));
        RenderUtil.underline(g, px, nameY + font.lineHeight + 3, 14, Colors.fade(Theme.ACCENT_LIGHT, alpha));
    }

    private static String modVersion() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(ArchangelClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (hasIntro && introTime() < INTRO_END) {
            skipIntro();
            return true;
        }
        return super.mouseClicked(event, doubleClick) || preview.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return preview.mouseDragged(event.x(), event.y(), event.button()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return preview.mouseReleased(event.x(), event.y(), event.button()) || super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (hasIntro && introTime() < INTRO_END) {
            skipIntro();
            return true;
        }
        return super.keyPressed(event);
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

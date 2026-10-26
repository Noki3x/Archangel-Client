package dev.archangelclient.gui.screen;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.config.ConfigManager;
import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.Setting;
import dev.archangelclient.core.setting.StringSetting;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.MenuBackground;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Easing;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.gui.widget.KeybindButton;
import dev.archangelclient.gui.widget.ModuleCard;
import dev.archangelclient.gui.widget.ScrollPanel;
import dev.archangelclient.gui.widget.SettingWidget;
import dev.archangelclient.gui.widget.TextButton;
import dev.archangelclient.gui.widget.TextField;
import dev.archangelclient.gui.widget.Toggle;
import dev.archangelclient.gui.widget.UiWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

/**
 * Client settings: a category sidebar, a searchable grid of module cards, and a settings page per
 * module generated from its {@link Setting}s. Also hosts the profile manager.
 * Opens from the main menu's "Client" button and in game with Right Shift.
 */
public class ClientSettingsScreen extends FadingScreen {
    private static final Identifier LOGO = ArchangelClient.id("textures/gui/logo.png");
    private static final long STATUS_MS = 4000;

    private enum View { GRID, MODULE, PROFILES }

    private final Screen parent;
    private final List<UiWidget> chrome = new ArrayList<>();
    private final ScrollPanel content = new ScrollPanel();
    private final TextField search = new TextField("Search modules…", 32);
    private TextField profileName;

    private View view = View.GRID;
    private Category category = Category.HUD;
    private Module selected;
    private long viewChangedAt;
    private String status;
    private long statusAt;

    // panel geometry, recomputed in init()
    private int px, py, pw, ph, cx, cw, contentTop;

    public ClientSettingsScreen(Screen parent) {
        this(parent, null);
    }

    /** Opens straight onto {@code module}'s settings page (used by the HUD editor's right-click). */
    public ClientSettingsScreen(Screen parent, Module module) {
        super(Component.literal("Client Settings"));
        this.parent = parent;
        if (module != null) {
            category = module.getCategory();
            view = View.MODULE;
            selected = module;
        }
        search.onChange(s -> {
            if (view != View.GRID) view = View.GRID;
            rebuildContent();
        });
    }

    @Override
    protected void init() {
        super.init();
        pw = Math.min(width - 48, Theme.SETTINGS_MAX_WIDTH);
        ph = Math.min(height - 40, Theme.SETTINGS_MAX_HEIGHT);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx = px + Theme.SIDEBAR_WIDTH + 12;
        cw = pw - Theme.SIDEBAR_WIDTH - 24 - 6;
        contentTop = py + 36;

        chrome.clear();
        int iy = py + 40;
        for (Category c : Category.values()) {
            chrome.add(new SidebarItem(c.getDisplayName(), () -> view != View.PROFILES && search.getText().isEmpty() && category == c,
                    () -> openCategory(c)).setBounds(px + 8, iy, Theme.SIDEBAR_WIDTH - 16, 17));
            iy += 19;
        }
        iy += 6;
        chrome.add(new SidebarItem("Profiles", () -> view == View.PROFILES, this::openProfiles)
                .setBounds(px + 8, iy, Theme.SIDEBAR_WIDTH - 16, 17));
        chrome.add(new TextButton("Done", TextButton.Style.NORMAL, this::onClose)
                .setBounds(px + 8, py + ph - 24, Theme.SIDEBAR_WIDTH - 16, 16));

        int searchW = Math.min(140, cw / 2);
        search.setBounds(cx + cw - searchW, py + 10, searchW, 18);
        content.setBounds(cx, contentTop, cw, py + ph - 10 - contentTop);
        rebuildContent();
    }

    // --- navigation

    private void openCategory(Category c) {
        category = c;
        search.setText("");
        search.setFocused(false);
        switchView(View.GRID, null);
    }

    private void openModule(Module m) {
        search.setFocused(false);
        switchView(View.MODULE, m);
    }

    private void openProfiles() {
        search.setText("");
        search.setFocused(false);
        switchView(View.PROFILES, null);
    }

    private void switchView(View v, Module m) {
        view = v;
        selected = m;
        viewChangedAt = Util.getMillis();
        KeybindButton.stopListening();
        rebuildContent();
        content.scrollToTop();
    }

    private void rebuildContent() {
        if (minecraft == null) return;
        content.clear();
        chrome.removeIf(w -> w instanceof HeaderWidget);
        switch (view) {
            case GRID -> buildGrid();
            case MODULE -> buildModulePage();
            case PROFILES -> buildProfiles();
        }
    }

    private void buildGrid() {
        if (category == Category.HUD && search.getText().isBlank()) {
            TextButton edit = new TextButton("Edit HUD Layout", TextButton.Style.ACCENT, () -> minecraft.setScreen(new HudEditorScreen(this)));
            chrome.add(new HeaderWidget(edit).setBounds(search.getX() - edit.preferredWidth() - Theme.GAP, py + 11, edit.preferredWidth(), 16));
        }
        if (category == Category.COSMETICS && search.getText().isBlank()) {
            TextButton open = new TextButton("Open Locker", TextButton.Style.ACCENT, () -> minecraft.setScreen(new CosmeticsScreen(this)));
            chrome.add(new HeaderWidget(open).setBounds(search.getX() - open.preferredWidth() - Theme.GAP, py + 11, open.preferredWidth(), 16));
        }
        List<Module> modules = search.getText().isBlank()
                ? ArchangelClient.modules().byCategory(category)
                : ArchangelClient.modules().search(search.getText());
        int cols = cw >= 360 ? 3 : 2;
        int cardW = (cw - (cols - 1) * Theme.GAP) / cols;
        for (int i = 0; i < modules.size(); i++) {
            int col = i % cols, row = i / cols;
            content.add(new ModuleCard(modules.get(i), this::openModule))
                    .setBounds(col * (cardW + Theme.GAP), row * (Theme.CARD_HEIGHT + Theme.GAP), cardW, Theme.CARD_HEIGHT);
        }
    }

    private void buildModulePage() {
        Module m = selected;
        chrome.add(new HeaderWidget(new TextButton("Back", TextButton.Style.NORMAL, () -> switchView(View.GRID, null)))
                .setBounds(cx, py + 11, 40, 16));
        chrome.add(new HeaderWidget(new Toggle(m::isEnabled, m::toggle))
                .setBounds(cx + cw - Toggle.WIDTH, py + 13, Toggle.WIDTH, Toggle.HEIGHT));
        for (Setting<?> s : m.getSettings()) {
            if (s instanceof StringSetting) continue; // stored-only values, edited from their own screens
            content.add(SettingWidget.of(s));
        }
        content.add(SettingWidget.of(m.getKeybind()));
        content.add(new TextButton("Reset to defaults", TextButton.Style.NORMAL, () -> {
            m.getSettings().forEach(Setting::reset);
            m.getKeybind().reset();
        }));
    }

    private void buildProfiles() {
        ConfigManager config = ArchangelClient.config();
        int y = 0;
        for (String name : config.listProfiles()) {
            content.add(new ProfileRow(name)).setBounds(0, y, cw, 22);
            y += 24;
        }
        y += 10;
        profileName = content.add(new TextField("New profile name", 24));
        profileName.setBounds(0, y, cw - 76, 18);
        content.add(new TextButton("Create", TextButton.Style.ACCENT, () -> {
            String name = ConfigManager.sanitizeName(profileName.getText());
            if (name.isEmpty()) setStatus("Enter a name using letters, numbers, spaces, - or _");
            else if (config.create(name)) setStatus("Created and switched to '" + name + "'");
            else setStatus("A profile called '" + name + "' already exists");
            rebuildContent();
        })).setBounds(cw - 70, y + 1, 70, 16);
        y += 30;
        int third = (cw - 2 * Theme.GAP) / 3;
        content.add(new TextButton("Export", TextButton.Style.NORMAL, () -> {
            minecraft.keyboardHandler.setClipboard(config.exportActive());
            setStatus("Copied to clipboard and saved in config/archangelclient/exports");
        })).setBounds(0, y, third, 16);
        content.add(new TextButton("Import from clipboard", TextButton.Style.NORMAL, () -> {
            String name = config.importProfile(minecraft.keyboardHandler.getClipboard(), "imported");
            setStatus(name == null ? "The clipboard doesn't contain an Archangel profile" : "Imported as '" + name + "'");
            rebuildContent();
        })).setBounds(third + Theme.GAP, y, third, 16);
        content.add(new TextButton("Open folder", TextButton.Style.NORMAL,
                () -> Util.getPlatform().openPath(config.getDirectory())))
                .setBounds(2 * (third + Theme.GAP), y, third, 16);
    }

    private void setStatus(String message) {
        status = message;
        statusAt = Util.getMillis();
    }

    /** Stacks the module page's rows, whose heights change as dropdowns and pickers open. */
    private void layoutModulePage() {
        int y = 0;
        for (UiWidget w : content.children()) {
            if (!w.isVisible()) continue;
            if (w instanceof TextButton b) {
                b.setBounds(0, y + 8, b.preferredWidth(), 16);
                y += 28;
            } else {
                w.setBounds(0, y, cw, Theme.ROW_HEIGHT);
                y += w.getHeight() + 2;
            }
        }
    }

    // --- rendering

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) MenuBackground.render(g, width, height, 1f);
        else super.renderBackground(g, mouseX, mouseY, partialTick); // vanilla blur over the world
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float open = fade();
        g.pose().pushMatrix();
        g.pose().translate(0, (1 - open) * 10);

        RenderUtil.glow(g, px, py, pw, ph, Theme.RADIUS_LARGE, Theme.ACCENT_PURPLE, 0.5f);
        RenderUtil.roundedRect(g, px, py, pw, ph, Theme.RADIUS_LARGE, Theme.PANEL_SOLID);
        RenderUtil.roundedRect(g, px, py, Theme.SIDEBAR_WIDTH, ph, Theme.RADIUS_LARGE, Theme.SIDEBAR);
        g.fill(px + Theme.SIDEBAR_WIDTH, py + 8, px + Theme.SIDEBAR_WIDTH + 1, py + ph - 8, Theme.DIVIDER);

        // sidebar brand
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, px + 10, py + 11, 0, 0, 16, 16, 1024, 1024, 1024, 1024);
        Fonts.drawBold(g, "Archangel", px + 30, py + 15, Theme.TEXT);
        Fonts.draw(g, Fonts.trim("Profile: " + ArchangelClient.config().getActiveProfile(), Theme.SIDEBAR_WIDTH - 18),
                px + 10, py + ph - 38, Theme.TEXT_DIM);

        renderHeader(g);
        for (UiWidget w : chrome) w.render(g, mouseX, mouseY, partialTick);
        if (view == View.GRID) search.render(g, mouseX, mouseY, partialTick);

        // content slides in from the right whenever the view changes
        float t = Easing.outCubic((Util.getMillis() - viewChangedAt) / (float) Theme.VIEW_SLIDE_MS);
        if (view == View.MODULE) layoutModulePage();
        g.pose().pushMatrix();
        g.pose().translate((1 - t) * 14, 0);
        content.render(g, mouseX, mouseY, partialTick);
        g.pose().popMatrix();
        if (t < 1) {
            g.fill(cx - 2, contentTop, cx + cw + 8, py + ph - 8, Colors.withAlpha(Theme.PANEL_SOLID, Math.round(0xE6 * (1 - t))));
        }

        renderEmptyState(g);
        renderStatus(g);
        g.pose().popMatrix();
    }

    private void renderHeader(GuiGraphics g) {
        switch (view) {
            case GRID -> {
                String title = search.getText().isBlank() ? category.getDisplayName() : "Search results";
                Fonts.drawBold(g, title, cx, py + 15, Theme.TEXT);
            }
            case MODULE -> {
                int tx = cx + 48;
                Fonts.drawBold(g, Fonts.trim(selected.getName(), cw - 90), tx, py + 10, Theme.TEXT);
                Fonts.draw(g, Fonts.trim(selected.getDescription(), cw - 90), tx, py + 21, Theme.TEXT_MUTED);
            }
            case PROFILES -> {
                Fonts.drawBold(g, "Profiles", cx, py + 10, Theme.TEXT);
                Fonts.draw(g, "Settings are saved automatically to the active profile", cx, py + 21, Theme.TEXT_MUTED);
            }
        }
    }

    private void renderEmptyState(GuiGraphics g) {
        if (view != View.GRID || !content.children().isEmpty()) return;
        int midY = contentTop + (py + ph - contentTop) / 2 - 10;
        int mid = cx + cw / 2;
        if (!search.getText().isBlank()) {
            Fonts.drawCentered(g, "No modules match \"" + Fonts.trim(search.getText(), 120) + "\"", mid, midY, Theme.TEXT_MUTED);
        } else {
            Fonts.drawCentered(g, category.getDisplayName() + " features are on the way", mid, midY, Theme.TEXT_MUTED);
            Fonts.drawCentered(g, "They arrive in an upcoming update", mid, midY + 12, Theme.TEXT_DIM);
        }
    }

    private void renderStatus(GuiGraphics g) {
        if (status == null) return;
        long age = Util.getMillis() - statusAt;
        if (age > STATUS_MS) {
            status = null;
            return;
        }
        float a = 1 - Easing.clamp01((age - (STATUS_MS - 400)) / 400f);
        String text = Fonts.trim(status, cw - 16);
        int w = Fonts.width(text) + 16;
        int sx = cx + (cw - w) / 2, sy = py + ph - 26;
        RenderUtil.roundedRect(g, sx, sy, w, 16, Theme.RADIUS, Colors.fade(Colors.withAlpha(Theme.ACCENT, 0xD0), a));
        Fonts.drawCentered(g, text, sx + w / 2, sy + 4, Colors.fade(Theme.TEXT, a));
    }

    // --- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        boolean handled = view == View.GRID && search.mouseClicked(mx, my, button);
        if (!handled) {
            for (UiWidget w : List.copyOf(chrome)) {
                if (w.mouseClicked(mx, my, button)) {
                    handled = true;
                    break;
                }
            }
        }
        if (handled) {
            content.mouseClicked(Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, button);
            return true;
        }
        return content.mouseClicked(mx, my, button) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return content.mouseReleased(event.x(), event.y(), event.button()) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return content.mouseDragged(event.x(), event.y(), event.button()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        content.scrollBy(scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (KeybindButton.isAnyListening()) return content.keyPressed(key, event.scancode(), event.modifiers());
        if (search.isFocused()) return search.keyPressed(key, event.scancode(), event.modifiers());
        if (content.keyPressed(key, event.scancode(), event.modifiers())) return true;
        boolean ctrl = (event.modifiers() & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        if (ctrl && key == GLFW.GLFW_KEY_F) {
            if (view != View.GRID) switchView(View.GRID, null);
            search.setFocused(true);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && view == View.MODULE) {
            switchView(View.GRID, null);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (search.charTyped(cp) || content.charTyped(cp)) return true;
        // typing on the module grid starts a search
        if (view == View.GRID && !Character.isWhitespace(cp) && !Character.isISOControl(cp)) {
            search.setFocused(true);
            return search.charTyped(cp);
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        KeybindButton.stopListening();
        ArchangelClient.config().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- small screen-local widgets

    /** Sidebar entry: muted text, a hover bar, and an accent pill when selected. */
    private static final class SidebarItem extends UiWidget {
        private final String label;
        private final BooleanSupplier selected;
        private final Runnable onClick;
        private final Animation hover = new Animation(Theme.HOVER_MS, 0f);
        private final Animation active = new Animation(Theme.HOVER_MS, 0f);

        SidebarItem(String label, BooleanSupplier selected, Runnable onClick) {
            this.label = label;
            this.selected = selected;
            this.onClick = onClick;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            hover.target(isHovered(mouseX, mouseY) ? 1f : 0f);
            active.target(selected.getAsBoolean() ? 1f : 0f);
            float h = hover.get(), a = active.get();
            if (h > 0.01f) RenderUtil.roundedRect(g, x, y, width, height, Theme.RADIUS, Colors.fade(Theme.HOVER_BAR, h * 0.6f));
            if (a > 0.01f) {
                RenderUtil.roundedRect(g, x, y, width, height, Theme.RADIUS, Colors.fade(Colors.withAlpha(Theme.ACCENT, 0x38), a));
                RenderUtil.glow(g, x, y + 4, 2, height - 8, 1, Theme.ACCENT_LIGHT, a);
                g.fill(x, y + 4, x + 2, y + height - 4, Colors.fade(Theme.ACCENT_LIGHT, a));
            }
            int color = Colors.lerp(Colors.lerp(Theme.TEXT_MUTED, Theme.TEXT, h), Theme.TEXT, a);
            Fonts.draw(g, label, x + 9, y + (height - 8) / 2, color);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !isHovered(mouseX, mouseY)) return false;
            onClick.run();
            return true;
        }
    }

    /** Wraps a widget that belongs to the current view's header, so it's removed on view change. */
    private static final class HeaderWidget extends UiWidget {
        private final UiWidget inner;

        HeaderWidget(UiWidget inner) {
            this.inner = inner;
        }

        @Override
        public UiWidget setBounds(int x, int y, int width, int height) {
            inner.setBounds(x, y, width, height);
            return super.setBounds(x, y, width, height);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            inner.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return inner.mouseClicked(mouseX, mouseY, button);
        }
    }

    /** One saved profile: name, an "Active" badge or Load button, and Delete. */
    private final class ProfileRow extends UiWidget {
        private final String name;
        private final TextButton load;
        private final TextButton delete;

        ProfileRow(String name) {
            this.name = name;
            ConfigManager config = ArchangelClient.config();
            this.load = new TextButton("Load", TextButton.Style.ACCENT, () -> {
                config.switchTo(name);
                setStatus("Switched to '" + name + "'");
                rebuildContent();
            });
            this.delete = new TextButton("Delete", TextButton.Style.DANGER, () -> {
                if (config.delete(name)) setStatus("Deleted '" + name + "'");
                rebuildContent();
            });
        }

        private boolean isActive() {
            return name.equals(ArchangelClient.config().getActiveProfile());
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            RenderUtil.roundedRect(g, x, y, width, height, Theme.RADIUS, isHovered(mouseX, mouseY) ? Theme.CARD_HOVER : Theme.CARD);
            Fonts.draw(g, Fonts.trim(name, width - 140), x + 9, y + 7, Theme.TEXT);
            if (isActive()) {
                int bw = Fonts.width("Active") + 12;
                int bx = x + width - bw - 6;
                RenderUtil.roundedRect(g, bx, y + 4, bw, 14, 7, Colors.withAlpha(Theme.ACCENT, 0x40));
                Fonts.drawCentered(g, "Active", bx + bw / 2, y + 7, Theme.ACCENT_LIGHT);
            } else {
                delete.setBounds(x + width - 54, y + 3, 48, 16);
                load.setBounds(x + width - 108, y + 3, 48, 16);
                load.render(g, mouseX, mouseY, partialTick);
                delete.render(g, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isActive()) return false;
            return load.mouseClicked(mouseX, mouseY, button) || delete.mouseClicked(mouseX, mouseY, button);
        }
    }
}

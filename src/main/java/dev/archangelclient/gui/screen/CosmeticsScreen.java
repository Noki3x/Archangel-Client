package dev.archangelclient.gui.screen;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.cosmetics.Cosmetic;
import dev.archangelclient.cosmetics.CosmeticManager;
import dev.archangelclient.cosmetics.CosmeticType;
import dev.archangelclient.cosmetics.Loadout;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.MenuBackground;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Easing;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.gui.widget.CosmeticCard;
import dev.archangelclient.gui.widget.CosmeticPreview;
import dev.archangelclient.gui.widget.ScrollPanel;
import dev.archangelclient.gui.widget.TextButton;
import dev.archangelclient.gui.widget.UiWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Cosmetics locker: a live 3D preview on the left, the owned cosmetics of the chosen slot on the
 * right. Selecting a cosmetic tries it on in the preview; Equip (or a double click) wears it.
 * Opens from the main menu and from the Cosmetics category of the client settings.
 */
public class CosmeticsScreen extends FadingScreen {
    private static final int CARD_HEIGHT = 46;
    private static final int TAB_HEIGHT = 16;

    private final Screen parent;
    private final CosmeticPreview preview = new CosmeticPreview(true);
    private final ScrollPanel grid = new ScrollPanel();
    private final List<UiWidget> chrome = new ArrayList<>();
    private CosmeticType tab = CosmeticType.CAPE;
    private @Nullable Cosmetic selected;
    private long tabChangedAt;
    private TextButton equipButton;

    private int px, py, pw, ph, previewW, gx, gw;

    public CosmeticsScreen(Screen parent) {
        super(Component.literal("Cosmetics"));
        this.parent = parent;
    }

    private static CosmeticManager cosmetics() {
        return ArchangelClient.cosmetics();
    }

    @Override
    protected void init() {
        super.init();
        pw = Math.min(width - 32, 580);
        ph = Math.min(height - 32, 330);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        previewW = Math.max(120, pw * 34 / 100);
        gx = px + previewW + 12;
        gw = pw - previewW - 24;

        chrome.clear();
        int tx = gx;
        for (CosmeticType type : CosmeticType.values()) {
            String label = type == CosmeticType.TRAIL ? "Trails" : type.plural();
            Tab t = new Tab(type, label);
            int w = Fonts.width(label) + 12;
            t.setBounds(tx, py + 38, w, TAB_HEIGHT);
            chrome.add(t);
            tx += w + 3;
        }
        equipButton = new TextButton(this::equipLabel, TextButton.Style.ACCENT, this::toggleSelected);
        chrome.add(equipButton.setBounds(px + 12, py + ph - 26, previewW - 24 - 54, 16));
        chrome.add(new TextButton("Done", TextButton.Style.NORMAL, this::onClose).setBounds(px + previewW - 12 - 50, py + ph - 26, 50, 16));

        preview.setBounds(px + 10, py + 28, previewW - 20, ph - 28 - 86);
        grid.setBounds(gx, py + 62, gw, ph - 62 - 10);
        rebuildGrid();
    }

    private void rebuildGrid() {
        grid.clear();
        tabChangedAt = Util.getMillis();
        List<Cosmetic> items = new ArrayList<>();
        for (Cosmetic c : cosmetics().registry().byType(tab)) if (cosmetics().isOwned(c)) items.add(c);
        int cols = gw >= 300 ? 2 : 1;
        int cardW = (gw - (cols - 1) * Theme.GAP) / cols;
        for (int i = 0; i < items.size(); i++) {
            Cosmetic c = items.get(i);
            grid.add(new CosmeticCard(c, () -> selected == c, this::select, this::toggle))
                    .setBounds((i % cols) * (cardW + Theme.GAP), (i / cols) * (CARD_HEIGHT + Theme.GAP), cardW, CARD_HEIGHT);
        }
        if (selected == null || selected.type() != tab) {
            selected = null;
            String equipped = cosmetics().localLoadout().get(tab);
            for (Cosmetic c : items) if (c.id().equals(equipped)) selected = c;
        }
    }

    private void select(Cosmetic c) {
        selected = c;
    }

    private void toggle(Cosmetic c) {
        selected = c;
        cosmetics().equip(c.type(), cosmetics().isEquipped(c) ? null : c.id());
    }

    private void toggleSelected() {
        if (selected != null) toggle(selected);
    }

    private String equipLabel() {
        if (selected == null) return "Select a cosmetic";
        return cosmetics().isEquipped(selected) ? "Unequip" : "Equip";
    }

    /** The equipped loadout with the selected cosmetic tried on. */
    private Loadout previewLoadout() {
        Loadout l = cosmetics().localLoadout();
        return selected != null ? l.with(selected.type(), selected.id()) : l;
    }

    // --- rendering

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) MenuBackground.render(g, width, height, 1f);
        else super.renderBackground(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float open = fade();
        int slide = Math.round((1 - open) * 10);
        g.pose().pushMatrix();
        g.pose().translate(0, slide);

        RenderUtil.glow(g, px, py, pw, ph, Theme.RADIUS_LARGE, Theme.ACCENT_PURPLE, 0.5f);
        RenderUtil.roundedRect(g, px, py, pw, ph, Theme.RADIUS_LARGE, Theme.PANEL_SOLID);
        RenderUtil.roundedRect(g, px, py, previewW, ph, Theme.RADIUS_LARGE, Theme.SIDEBAR);
        g.fill(px + previewW, py + 8, px + previewW + 1, py + ph - 8, Theme.DIVIDER);

        Fonts.drawBold(g, "Locker", px + 12, py + 12, Theme.TEXT);
        Fonts.draw(g, "Drag to rotate", px + previewW - 12 - Fonts.width("Drag to rotate"), py + 12, Theme.TEXT_DIM);
        Fonts.drawBold(g, "Cosmetics", gx, py + 10, Theme.TEXT);
        String status = ArchangelClient.profiles().isOffline()
                ? "Offline mode: every cosmetic is unlocked and only you can see them"
                : "Other Archangel players can see what you equip";
        Fonts.draw(g, Fonts.trim(status, gw), gx, py + 21, Theme.TEXT_MUTED);

        renderSelectedInfo(g);
        for (UiWidget w : chrome) w.render(g, mouseX, mouseY, partialTick);

        float t = Easing.outCubic((Util.getMillis() - tabChangedAt) / (float) Theme.VIEW_SLIDE_MS);
        g.pose().pushMatrix();
        g.pose().translate((1 - t) * 14, 0);
        grid.render(g, mouseX, mouseY, partialTick);
        g.pose().popMatrix();
        if (grid.children().isEmpty()) {
            Fonts.drawCentered(g, "No " + tab.plural().toLowerCase() + " yet", gx + gw / 2, py + ph / 2, Theme.TEXT_MUTED);
        }
        veil(g, px + previewW, py, pw - previewW, ph);
        g.pose().popMatrix();

        preview.setLoadout(previewLoadout());
        preview.setRenderOffset(0, slide);
        preview.render(g, mouseX, mouseY, partialTick);
    }

    private void renderSelectedInfo(GuiGraphics g) {
        int ix = px + 12, iy = py + ph - 80, iw = previewW - 24;
        if (selected == null) {
            Fonts.draw(g, "Pick a cosmetic to try it on", ix, iy + 8, Theme.TEXT_MUTED);
            return;
        }
        Fonts.drawBold(g, Fonts.trim(selected.name(), iw), ix, iy, Theme.TEXT);
        Fonts.draw(g, selected.rarity().displayName() + " " + selected.type().singular().toLowerCase(), ix, iy + 11, selected.rarity().color());
        List<FormattedCharSequence> lines = Fonts.wrap(selected.description(), iw);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.drawString(Fonts.font(), lines.get(i), ix, iy + 22 + i * 9, Theme.TEXT_DIM, false);
        }
    }

    // --- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        for (UiWidget w : List.copyOf(chrome)) if (w.mouseClicked(mx, my, button)) return true;
        if (preview.mouseClicked(mx, my, button)) return true;
        return grid.mouseClicked(mx, my, button) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        preview.mouseReleased(event.x(), event.y(), event.button());
        return grid.mouseReleased(event.x(), event.y(), event.button()) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (preview.mouseDragged(event.x(), event.y(), event.button())) return true;
        return grid.mouseDragged(event.x(), event.y(), event.button()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        grid.scrollBy(scrollY);
        return true;
    }

    @Override
    public void onClose() {
        ArchangelClient.config().save();
        fadeOutThen(() -> minecraft.setScreen(parent));
    }

    /** Pill tab for one slot; the active one is filled with the accent color. */
    private final class Tab extends UiWidget {
        private final CosmeticType type;
        private final String label;
        private final Animation active;
        private final Animation hover = new Animation(Theme.HOVER_MS, 0f);

        Tab(CosmeticType type, String label) {
            this.type = type;
            this.label = label;
            this.active = new Animation(Theme.HOVER_MS, tab == type ? 1f : 0f);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            active.target(tab == type ? 1f : 0f);
            hover.target(isHovered(mouseX, mouseY) ? 1f : 0f);
            float a = active.get(), h = hover.get();
            int bg = Colors.lerp(Colors.lerp(Theme.FIELD, Theme.HOVER_BAR, h), Colors.withAlpha(Theme.ACCENT, 0xD0), a);
            if (a > 0.05f) RenderUtil.glow(g, x, y, width, height, height / 2, Theme.ACCENT, a * 0.6f);
            RenderUtil.roundedRect(g, x, y, width, height, height / 2, bg);
            boolean worn = cosmetics().localLoadout().get(type) != null;
            Fonts.drawCentered(g, label, x + width / 2, y + 4, Colors.lerp(Theme.TEXT_MUTED, Theme.TEXT, Math.max(a, h)));
            if (worn) g.fill(x + width - 5, y + 3, x + width - 3, y + 5, Theme.ACCENT_LIGHT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !isHovered(mouseX, mouseY)) return false;
            if (tab != type) {
                tab = type;
                rebuildGrid();
            }
            return true;
        }
    }
}

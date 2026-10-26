package dev.archangelclient.gui.widget;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.cosmetics.Cosmetic;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Grid card for one cosmetic: a swatch of its texture or colors, name, rarity and an equipped
 * badge. Click selects (try-on in the preview); double click equips.
 */
public class CosmeticCard extends UiWidget {
    private static final long DOUBLE_CLICK_MS = 300;

    private final Cosmetic cosmetic;
    private final BooleanSupplier selected;
    private final Consumer<Cosmetic> onSelect;
    private final Consumer<Cosmetic> onEquip;
    private final Animation hover = new Animation(Theme.HOVER_MS, 0f);
    private final Animation equipped;
    private long lastClick;

    public CosmeticCard(Cosmetic cosmetic, BooleanSupplier selected, Consumer<Cosmetic> onSelect, Consumer<Cosmetic> onEquip) {
        this.cosmetic = cosmetic;
        this.selected = selected;
        this.onSelect = onSelect;
        this.onEquip = onEquip;
        this.equipped = new Animation(Theme.HOVER_MS, isEquipped() ? 1f : 0f);
    }

    private boolean isEquipped() {
        return ArchangelClient.cosmetics().isEquipped(cosmetic);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hover.target(isHovered(mouseX, mouseY) || selected.getAsBoolean() ? 1f : 0f);
        equipped.target(isEquipped() ? 1f : 0f);
        float h = hover.get(), e = equipped.get();
        int rarity = cosmetic.rarity().color();

        if (e > 0.02f) RenderUtil.glow(g, x, y, width, height, Theme.RADIUS_LARGE, Theme.ACCENT, e * 0.4f);
        RenderUtil.roundedRect(g, x, y, width, height, Theme.RADIUS_LARGE, Colors.lerp(Theme.CARD, Theme.CARD_HOVER, h));
        if (selected.getAsBoolean()) RenderUtil.roundedRect(g, x, y + height - 2, width, 2, 1, Colors.withAlpha(Theme.ACCENT_LIGHT, 0xC0));

        // swatch
        int sx = x + 7, sy = y + 7, ss = height - 14;
        RenderUtil.roundedRect(g, sx, sy, ss, ss, Theme.RADIUS, 0x30000000);
        Identifier tex = cosmetic.texture(0);
        switch (cosmetic.type()) {
            case CAPE -> g.blit(RenderPipelines.GUI_TEXTURED, tex, sx + (ss - ss * 10 / 16) / 2, sy, 4, 4, ss * 10 / 16, ss, 40, 64, 256, 128);
            case WINGS -> g.blit(RenderPipelines.GUI_TEXTURED, tex, sx + 1, sy + ss / 5, 0, 0, ss - 2, (ss - 2) * 96 / 160, 160, 96, 256, 256);
            default -> {
                RenderUtil.roundedRect(g, sx + 5, sy + 5, ss - 10, ss - 10, Theme.RADIUS, cosmetic.primaryColor());
                RenderUtil.roundedRect(g, sx + ss / 2, sy + ss / 2, ss / 2 - 5, ss / 2 - 5, Theme.RADIUS, cosmetic.secondaryColor());
            }
        }

        int tx = sx + ss + 7, tw = x + width - tx - 6;
        Fonts.drawBold(g, Fonts.trim(cosmetic.name(), tw), tx, y + 8, Theme.TEXT);
        Fonts.draw(g, Fonts.trim(cosmetic.rarity().displayName() + " " + cosmetic.type().singular().toLowerCase(), tw), tx, y + 19, rarity);
        if (e > 0.02f) {
            String badge = "Equipped";
            int bw = Fonts.width(badge) + 10;
            RenderUtil.roundedRect(g, tx, y + height - 18, bw, 12, 6, Colors.fade(Colors.withAlpha(Theme.ACCENT, 0xD0), e));
            Fonts.draw(g, badge, tx + 5, y + height - 16, Colors.fade(Theme.TEXT, e));
        } else if (h > 0.02f) {
            Fonts.draw(g, "Double click to equip", tx, y + height - 15, Colors.fade(Theme.TEXT_DIM, h));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        long now = Util.getMillis();
        if (now - lastClick < DOUBLE_CLICK_MS) onEquip.accept(cosmetic);
        else onSelect.accept(cosmetic);
        lastClick = now;
        return true;
    }
}

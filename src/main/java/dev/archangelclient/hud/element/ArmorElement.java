package dev.archangelclient.hud.element;

import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ModeSetting;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.hud.HudElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Your worn armor (and held item) with durability. */
public class ArmorElement extends HudElement {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final ModeSetting layout = add(new ModeSetting("Layout", "Vertical or horizontal list", "Vertical", "Vertical", "Horizontal"));
    private final ModeSetting durability = add(new ModeSetting("Durability", "How durability is shown", "Percent", "Percent", "Value", "Hidden"));
    private final BooleanSetting heldItem = add(new BooleanSetting("Held item", "Also show the item in your main hand", true));

    public ArmorElement() {
        super("Armor Status", "Shows your armor and its durability", 1f, 0.55f, true);
    }

    @Override
    public void render(GuiGraphics g, float partialTick, boolean editor) {
        var player = mc().player;
        if (player == null) return;
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) items.add(player.getItemBySlot(slot));
        if (heldItem.get()) items.add(player.getMainHandItem());
        if (!editor) items.removeIf(ItemStack::isEmpty);
        if (items.isEmpty()) {
            width = height = 0;
            return;
        }

        boolean vertical = layout.is("Vertical");
        int textW = 0;
        for (ItemStack s : items) textW = Math.max(textW, Fonts.width(label(s)));
        int cell = vertical ? 18 : 18 + (textW > 0 ? textW + 2 : 0);
        width = vertical ? 4 + 16 + (textW > 0 ? 3 + textW : 0) + 4 : 4 + items.size() * cell;
        height = vertical ? 2 + items.size() * 18 : 20;
        panel(g, width, height);
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            int ix = vertical ? 4 : 4 + i * cell, iy = vertical ? 2 + i * 18 : 2;
            g.renderItem(s, ix, iy);
            g.renderItemDecorations(mc().font, s, ix, iy, "");
            String text = label(s);
            if (!text.isEmpty()) Fonts.draw(g, text, ix + 19, iy + 5, durabilityColor(s));
        }
    }

    private String label(ItemStack s) {
        if (s.isEmpty() || durability.is("Hidden")) return "";
        if (!s.isDamageableItem()) return s.getCount() > 1 ? String.valueOf(s.getCount()) : "";
        int left = s.getMaxDamage() - s.getDamageValue();
        return durability.is("Value") ? String.valueOf(left) : Math.round(left * 100f / s.getMaxDamage()) + "%";
    }

    private int durabilityColor(ItemStack s) {
        if (!s.isDamageableItem()) return textColor.get();
        float f = 1f - s.getDamageValue() / (float) s.getMaxDamage();
        return f > 0.5f ? textColor.get() : Colors.lerp(0xFFEF4444, 0xFFFACC15, f * 2);
    }
}

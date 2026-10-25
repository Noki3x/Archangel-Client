package dev.archangelclient.hud.element;

import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.hud.HudElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;

/** Your active potion effects with level and time left. */
public class PotionsElement extends HudElement {
    private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
    private final BooleanSetting showIcons = add(new BooleanSetting("Icons", "Show the effect icons", true));

    public PotionsElement() {
        super("Potion Effects", "Lists your active potion effects and their time left", 1f, 0.3f, true);
    }

    @Override
    public void render(GuiGraphics g, float partialTick, boolean editor) {
        var player = mc().player;
        if (player == null) return;
        List<MobEffectInstance> effects = new ArrayList<>(player.getActiveEffects());
        if (effects.isEmpty() && editor) {
            effects.add(new MobEffectInstance(MobEffects.SPEED, 20 * 90, 1));
            effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 300));
        }
        if (effects.isEmpty()) {
            width = height = 0;
            return;
        }
        float tickRate = mc().level == null ? 20f : mc().level.tickRateManager().tickrate();
        int icon = showIcons.get() ? 18 : 0;
        int textW = 0;
        List<String[]> lines = new ArrayList<>();
        for (MobEffectInstance e : effects) {
            int amp = e.getAmplifier();
            String name = e.getEffect().value().getDisplayName().getString() + (amp > 0 && amp < ROMAN.length ? ROMAN[amp] : amp > 0 ? " " + (amp + 1) : "");
            String time = MobEffectUtil.formatDuration(e, 1f, tickRate).getString();
            lines.add(new String[] {name, time});
            textW = Math.max(textW, Math.max(Fonts.width(name), Fonts.width(time)));
        }
        width = 4 + icon + (icon > 0 ? 4 : 0) + textW + 6;
        height = 2 + effects.size() * 22;
        panel(g, width, height);
        for (int i = 0; i < effects.size(); i++) {
            int y = 2 + i * 22;
            if (icon > 0) g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effects.get(i).getEffect()), 4, y + 2, 18, 18);
            int tx = 4 + icon + (icon > 0 ? 4 : 0);
            text(g, lines.get(i)[0], tx, y + 2);
            Fonts.draw(g, lines.get(i)[1], tx, y + 12, Theme.TEXT_MUTED);
        }
    }
}

package dev.archangelclient.hud.element;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.NumberSetting;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import dev.archangelclient.hud.CombatTracker;
import dev.archangelclient.hud.HudElement;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/**
 * Panel about the entity you're fighting (reference photo 3): model preview, name, distance, an
 * animated health bar and a small graph of the damage it took.
 *
 * <p>Legit by design: it only appears after you hit an entity yourself, only while that entity is
 * in your direct line of sight and close, and hides a few seconds after combat. It never shows
 * anything through walls.
 */
public class TargetHudElement extends HudElement {
    private static final int W = 140;
    private static final int H = 48;
    private static final double MAX_DISTANCE = 8;

    private final NumberSetting hideAfter = add(new NumberSetting("Hide after", "Seconds after your last hit before the panel hides",
            2.5, 1, 3, 0.5).suffix(" s"));
    private final BooleanSetting showModel = add(new BooleanSetting("Model", "Show a 3D preview of the target", true));
    private final BooleanSetting showGraph = add(new BooleanSetting("Damage graph", "Show recent damage taken by the target", true));
    private final Animation visible = new Animation(Theme.SCREEN_FADE_MS, 0f);
    private float shownHealth = -1;
    private LivingEntity lastShown;
    private LivingEntity lastHealthEntity;

    public TargetHudElement() {
        super("Target HUD", "Shows the opponent you're fighting while they're in sight", 1f, 0.92f, true);
    }

    /** The target to show this frame, or null. */
    private LivingEntity activeTarget() {
        CombatTracker combat = ArchangelClient.combat();
        LivingEntity target = combat.getTarget();
        var player = mc().player;
        if (target == null || player == null || !target.isAlive()) return null;
        if (combat.msSinceAttack() > hideAfter.get() * 1000) return null;
        if (player.distanceTo(target) > MAX_DISTANCE || !player.hasLineOfSight(target)) return null;
        return target;
    }

    @Override
    public boolean shouldRender(boolean editor) {
        boolean active = activeTarget() != null;
        visible.target(active ? 1f : 0f);
        return editor || active || visible.get() > 0.02f;
    }

    @Override
    public void render(GuiGraphics g, float partialTick, boolean editor) {
        width = W;
        height = H;
        LivingEntity target = activeTarget();
        if (target != null) lastShown = target;
        // in the HUD editor with no fight going on, preview the panel using your own model
        boolean preview = editor && target == null;
        LivingEntity shown = preview ? mc().player : lastShown;
        if (shown == null) return;
        float alpha = preview ? 1f : visible.get();

        RenderUtil.glow(g, 0, 0, W, H, Theme.RADIUS_LARGE, Theme.ACCENT, 0.6f * alpha);
        RenderUtil.roundedRect(g, 0, 0, W, H, Theme.RADIUS_LARGE, Colors.fade(Theme.PANEL_SOLID, alpha));

        int left = 6;
        if (showModel.get()) {
            RenderUtil.roundedRect(g, 4, 4, 34, H - 8, Theme.RADIUS, Colors.fade(Theme.CARD, alpha));
            if (alpha > 0.5f) renderModel(g, shown);
            left = 44;
        }

        String name = preview ? "Opponent" : shown.getDisplayName().getString();
        Fonts.drawBold(g, Fonts.trim(name, W - left - 6), left, 6, Colors.fade(Theme.TEXT, alpha));
        double distance = preview ? 3.0 : mc().player.distanceTo(shown);
        Fonts.draw(g, String.format(Locale.ROOT, "Distance: %.1f", distance), left, 17, Colors.fade(Theme.TEXT_MUTED, alpha));

        // health bar eases toward the real value so hits visibly drain it
        float max = Math.max(1, shown.getMaxHealth());
        float health = preview ? max * 0.7f : shown.getHealth();
        if (shown != lastHealthEntity || shownHealth < 0) shownHealth = health;
        lastHealthEntity = shown;
        shownHealth += (health - shownHealth) * 0.15f;
        float f = Math.max(0, Math.min(1, shownHealth / max));
        int barX = left, barY = 30, barW = W - left - 8;
        RenderUtil.roundedRect(g, barX, barY, barW, 5, 2, Colors.fade(Theme.TOGGLE_OFF, alpha));
        int filled = Math.round(barW * f);
        if (filled > 1) {
            int color = healthColor(f);
            RenderUtil.glow(g, barX, barY, filled, 5, 2, color, 0.5f * alpha);
            RenderUtil.roundedRect(g, barX, barY, filled, 5, 2, Colors.fade(color, alpha));
        }
        String hp = String.format(Locale.ROOT, "%.1f / %.0f", health, max);
        Fonts.draw(g, hp, left, 38, Colors.fade(Theme.TEXT_MUTED, alpha));

        if (showGraph.get()) renderGraph(g, W - 8, 46, preview, alpha);
    }

    /**
     * Entity previews render into their own offscreen layer placed in absolute screen coordinates,
     * so map the model box through this element's position and scale by hand.
     */
    private void renderModel(GuiGraphics g, LivingEntity entity) {
        float s = getScale();
        int x1 = Math.round(placedX + 4 * s), y1 = Math.round(placedY + 4 * s);
        int x2 = Math.round(placedX + 38 * s), y2 = Math.round(placedY + (H - 4) * s);
        g.pose().pushMatrix();
        g.pose().identity();
        // a fixed "mouse" point up and to the right so the model looks into the panel
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, Math.round(19 * s), 0.0625f,
                x2 + 30 * s, y1 + 8 * s, entity);
        g.pose().popMatrix();
    }

    /** Small bar graph of recent hits, right-aligned to (right, bottom). */
    private void renderGraph(GuiGraphics g, int right, int bottom, boolean preview, float alpha) {
        List<Float> samples = preview ? List.of(2f, 3.5f, 1.5f, 4f, 2.5f, 3f) : List.copyOf(ArchangelClient.combat().getDamageSamples());
        if (samples.isEmpty()) return;
        float peak = 0;
        for (float s : samples) peak = Math.max(peak, s);
        int bar = 3, gap = 1, maxH = 8;
        int x = right - samples.size() * (bar + gap) + gap;
        for (float s : samples) {
            int h = Math.max(1, Math.round(maxH * s / Math.max(peak, 0.01f)));
            g.fill(x, bottom - 2 - h, x + bar, bottom - 2, Colors.fade(0xFFEF4444, alpha));
            x += bar + gap;
        }
    }

    /** Red at low health, through yellow, to green at full. */
    private static int healthColor(float f) {
        return f < 0.5f ? Colors.lerp(0xFFEF4444, 0xFFFACC15, f * 2) : Colors.lerp(0xFFFACC15, 0xFF22C55E, (f - 0.5f) * 2);
    }
}

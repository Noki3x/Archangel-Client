package dev.archangelclient.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.emotes.Emote;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.WheelTextures;
import dev.archangelclient.gui.theme.Animation;
import dev.archangelclient.gui.theme.Easing;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

/**
 * The emote wheel: hold the wheel key, point at an emote and let go to play it. A quick tap keeps
 * the wheel open so you can click instead. The middle cancels. The world stays visible, blurred.
 */
public class EmoteWheelScreen extends FadingScreen {
    /** Wedges shown at most; extra catalog entries wait for a future paging UI. */
    private static final int MAX_SLOTS = 8;
    private static final long TAP_MS = 250;
    private static final int ICON = 22;

    private final KeyMapping key;
    private final long openedMs = Util.getMillis();
    private final List<Emote> emotes = new ArrayList<>();
    private final List<Animation> hover = new ArrayList<>();
    private boolean sticky;
    private int hovered = -1;

    public EmoteWheelScreen(KeyMapping key) {
        super(Component.literal("Emotes"));
        this.key = key;
        List<Emote> all = ArchangelClient.emotes().registry().all();
        emotes.addAll(all.subList(0, Math.min(MAX_SLOTS, all.size())));
        for (int i = 0; i < emotes.size(); i++) hover.add(new Animation(Theme.HOVER_MS, 0));
    }

    /** Generates the wheel textures ahead of time, so the first open doesn't stall. */
    public static void prepare() {
        int count = Math.min(MAX_SLOTS, ArchangelClient.emotes().registry().all().size());
        if (count > 0) WheelTextures.wedge(count, 0);
        WheelTextures.disc();
        WheelTextures.glow();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int outerRadius() {
        return Math.max(60, Math.min(112, Math.round(height * 0.36f)));
    }

    private int innerRadius() {
        return Math.round(outerRadius() * WheelTextures.INNER);
    }

    private int slotAt(double mouseX, double mouseY) {
        if (emotes.isEmpty()) return -1;
        double dx = mouseX - width / 2.0, dy = mouseY - height / 2.0;
        if (dx * dx + dy * dy < Math.pow(innerRadius() * 0.8, 2)) return -1;
        double angle = Math.atan2(dx, -dy); // clockwise from up
        double span = Math.PI * 2 / emotes.size();
        int i = (int) Math.floor((angle + span / 2) / span);
        return Math.floorMod(i, emotes.size());
    }

    @Override
    public void tick() {
        if (sticky) return;
        if (!isHeld()) {
            if (hovered < 0 && Util.getMillis() - openedMs < TAP_MS) sticky = true;
            else select(hovered);
        }
    }

    private boolean isHeld() {
        InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(key);
        long window = minecraft.getWindow().handle();
        return switch (bound.getType()) {
            case KEYSYM -> GLFW.glfwGetKey(window, bound.getValue()) == GLFW.GLFW_PRESS;
            case MOUSE -> GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    private void select(int slot) {
        onClose();
        if (slot >= 0 && slot < emotes.size()) ArchangelClient.emotes().playLocal(emotes.get(slot));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            select(slotAt(event.x(), event.y()));
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // pressing the wheel key again while the wheel stays open closes it
        if (sticky && key.matches(event)) {
            onClose();
            return true;
        }
        // number keys pick a slot directly
        int n = event.key() - GLFW.GLFW_KEY_1;
        if (n >= 0 && n < emotes.size()) {
            select(n);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        hovered = slotAt(mouseX, mouseY);
        float open = fade();
        float pop = 0.86f + 0.14f * Easing.outCubic((Util.getMillis() - openedMs) / 180f);
        int cx = width / 2, cy = height / 2, outer = outerRadius(), inner = innerRadius();

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(pop, pop);

        // a soft dark pool behind the wheel keeps it readable over bright scenery
        drawCentered(g, WheelTextures.glow(), Math.round(outer * 1.6f), Colors.fade(0x90000000, open));
        // the GUI batches draws by texture within a stratum, so layer the wheel explicitly
        g.nextStratum();

        int count = emotes.size();
        for (int i = 0; i < count; i++) {
            Animation h = hover.get(i);
            h.target(i == hovered ? 1 : 0);
            float t = h.get();
            Identifier wedge = WheelTextures.wedge(count, i);
            if (t > 0.01f) {
                // soft accent glow behind the hovered wedge
                drawCentered(g, wedge, Math.round(outer * 1.09f), Colors.fade(Theme.ACCENT_PURPLE, 0.35f * t * open));
            }
        }
        g.nextStratum();
        for (int i = 0; i < count; i++) {
            float t = hover.get(i).get();
            float grow = 1 + 0.05f * t;
            int base = Colors.lerp(0xD01E1F24, 0xF03B82F6, t);
            drawCentered(g, WheelTextures.wedge(count, i), Math.round(outer * grow), Colors.fade(base, open));
        }
        g.nextStratum();
        for (int i = 0; i < count; i++) {
            Animation h = hover.get(i);
            float t = h.get();
            float grow = 1 + 0.05f * t;
            double angle = Math.PI * 2 * i / count;
            float r = (outer + inner) / 2f * grow;
            int ix = Math.round((float) Math.sin(angle) * r), iy = Math.round((float) -Math.cos(angle) * r);
            int iconColor = Colors.fade(Colors.lerp(0xFFC9CDD6, 0xFFFFFFFF, t), open);
            int size = Math.round(ICON * (1 + 0.12f * t));
            g.blit(RenderPipelines.GUI_TEXTURED, emotes.get(i).icon(), ix - size / 2, iy - size / 2 - 3, 0, 0,
                    size, size, 64, 64, 64, 64, iconColor);
            String label = Fonts.trim(emotes.get(i).name(), outer - inner);
            Fonts.drawCentered(g, label, ix, iy + size / 2 - 1, Colors.fade(Colors.lerp(Theme.TEXT_MUTED, Theme.TEXT, t), open));
        }

        // center: what will play, or how to use the wheel
        drawCentered(g, WheelTextures.disc(), inner - 6, Colors.fade(0xE616171B, open));
        g.nextStratum();
        Emote focus = hovered >= 0 ? emotes.get(hovered) : null;
        String title = focus != null ? focus.name() : (count == 0 ? "No emotes" : "Emotes");
        String hint = focus != null ? (sticky ? "Click to play" : "Release to play") : (sticky ? "Click an emote" : "Point at an emote");
        g.drawString(font, Fonts.bold(title), -font.width(Fonts.bold(title)) / 2, -9, Colors.fade(Theme.TEXT, open), false);
        Fonts.drawCentered(g, Fonts.trim(hint, inner * 2 - 16), 0, 3, Colors.fade(Theme.TEXT_MUTED, open));
        g.pose().popMatrix();

        String footer = "Moving cancels an emote  ·  1-" + Math.max(1, count) + " picks directly";
        Fonts.drawCentered(g, footer, cx, cy + Math.round(outer * pop) + 12, Colors.fade(Theme.TEXT_MUTED, open));
    }

    /** Draws a square texture centered on the current origin, {@code radius} from center to edge. */
    private static void drawCentered(GuiGraphics g, Identifier texture, int radius, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, -radius, -radius, 0, 0, radius * 2, radius * 2, 1, 1, 1, 1, color);
    }
}

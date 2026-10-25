package dev.archangelclient.hud;

import dev.archangelclient.core.module.ModuleManager;
import dev.archangelclient.gui.screen.HudEditorScreen;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Draws the enabled HUD elements each frame (the HUD editor draws them itself while it's open). */
public final class HudManager {
    private final ModuleManager modules;

    public HudManager(ModuleManager modules) {
        this.modules = modules;
    }

    public List<HudElement> elements() {
        return modules.all().stream().filter(m -> m instanceof HudElement).map(m -> (HudElement) m).toList();
    }

    public void render(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.screen instanceof HudEditorScreen) return;
        int w = g.guiWidth(), h = g.guiHeight();
        for (HudElement e : elements()) {
            if (e.isEnabled() && e.shouldRender(false)) e.renderPlaced(g, partialTick, false, w, h);
        }
    }

    public void resetLayout() {
        elements().forEach(HudElement::resetLayout);
    }
}

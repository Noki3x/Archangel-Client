package dev.archangelclient.core.event;

import net.minecraft.client.gui.GuiGraphics;

/** Posted while the in-game HUD renders, after the vanilla HUD layers. */
public record Render2DEvent(GuiGraphics graphics, float partialTick) {}

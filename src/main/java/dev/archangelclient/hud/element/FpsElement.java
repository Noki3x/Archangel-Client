package dev.archangelclient.hud.element;

import dev.archangelclient.hud.TextHudElement;

/** Frames per second. */
public class FpsElement extends TextHudElement {
    public FpsElement() {
        super("FPS", "Shows your frames per second", 0f, 0f, true);
    }

    @Override
    protected String text(boolean editor) {
        return mc().getFps() + " FPS";
    }
}

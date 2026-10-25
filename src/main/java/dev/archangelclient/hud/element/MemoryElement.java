package dev.archangelclient.hud.element;

import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.hud.TextHudElement;

/** Java heap usage. */
public class MemoryElement extends TextHudElement {
    private final BooleanSetting showMegabytes = add(new BooleanSetting("Show MB", "Show used and total megabytes", true));

    public MemoryElement() {
        super("Memory", "Shows how much memory the game is using", 1f, 0f, false);
    }

    @Override
    protected String text(boolean editor) {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) >> 20, max = rt.maxMemory() >> 20;
        String percent = "Mem " + (used * 100 / Math.max(1, max)) + "%";
        return showMegabytes.get() ? percent + "  " + used + "/" + max + " MB" : percent;
    }
}

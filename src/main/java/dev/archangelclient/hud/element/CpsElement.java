package dev.archangelclient.hud.element;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.hud.TextHudElement;

/** Clicks per second over the last second, left and optionally right. */
public class CpsElement extends TextHudElement {
    private final BooleanSetting showRight = add(new BooleanSetting("Right click", "Also show right clicks per second", true));

    public CpsElement() {
        super("CPS", "Shows your clicks per second", 0f, 0.06f, true);
    }

    @Override
    protected String text(boolean editor) {
        var clicks = ArchangelClient.clicks();
        return showRight.get() ? clicks.left() + " | " + clicks.right() + " CPS" : clicks.left() + " CPS";
    }
}

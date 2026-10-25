package dev.archangelclient.hud.element;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.hud.TextHudElement;

/** Hits landed in a row without being hit back (resets after 3 s without a hit). */
public class ComboElement extends TextHudElement {
    private final BooleanSetting hideZero = add(new BooleanSetting("Hide at zero", "Only show while you have a combo", false));

    public ComboElement() {
        super("Combo", "Counts hits landed in a row without taking a hit", 0.5f, 0.62f, false);
    }

    @Override
    public boolean shouldRender(boolean editor) {
        return editor || !hideZero.get() || ArchangelClient.combat().getCombo() > 0;
    }

    @Override
    protected String text(boolean editor) {
        int combo = ArchangelClient.combat().getCombo();
        return combo == 0 ? "No combo" : combo + " Combo";
    }
}

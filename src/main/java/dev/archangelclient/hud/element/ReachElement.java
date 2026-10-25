package dev.archangelclient.hud.element;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.hud.TextHudElement;
import java.util.Locale;

/**
 * Distance of your last hit, measured from your eyes to the target's hitbox. Display only: it
 * reads attacks that already went through vanilla's normal reach check.
 */
public class ReachElement extends TextHudElement {
    public ReachElement() {
        super("Reach Display", "Shows the distance of your last hit", 0.5f, 0.68f, false);
    }

    @Override
    protected String text(boolean editor) {
        double reach = ArchangelClient.combat().getLastReach();
        return reach < 0 ? "-- blocks" : String.format(Locale.ROOT, "%.2f blocks", reach);
    }
}

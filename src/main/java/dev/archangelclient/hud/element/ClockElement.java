package dev.archangelclient.hud.element;

import dev.archangelclient.core.setting.ModeSetting;
import dev.archangelclient.hud.TextHudElement;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Your computer's local time. */
public class ClockElement extends TextHudElement {
    private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private final ModeSetting format = add(new ModeSetting("Format", "12 or 24 hour clock", "24 hour", "24 hour", "12 hour"));

    public ClockElement() {
        super("Clock", "Shows the real-world time", 0.5f, 0f, false);
    }

    @Override
    protected String text(boolean editor) {
        return LocalTime.now().format(format.is("24 hour") ? H24 : H12);
    }
}

package dev.archangelclient.gui.screen;

import dev.archangelclient.gui.theme.Easing;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.util.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base for Archangel screens: fades in when opened. */
public abstract class FadingScreen extends Screen {
    private long openedAt;

    protected FadingScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        if (openedAt == 0) openedAt = Util.getMillis();
    }

    /** 0..1 opacity for the fade-in transition. */
    protected float fade() {
        return Easing.outCubic((Util.getMillis() - openedAt) / (float) Theme.SCREEN_FADE_MS);
    }
}

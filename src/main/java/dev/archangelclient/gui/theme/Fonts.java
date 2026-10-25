package dev.archangelclient.gui.theme;

import dev.archangelclient.ArchangelClient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * The bundled Inter font, rendered through Minecraft's TTF font provider
 * (see assets/archangelclient/font/*.json).
 */
public final class Fonts {
    private Fonts() {}

    public static final FontDescription INTER = new FontDescription.Resource(ArchangelClient.id("inter"));
    public static final FontDescription INTER_BOLD = new FontDescription.Resource(ArchangelClient.id("inter_bold"));

    public static MutableComponent regular(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(INTER));
    }

    public static MutableComponent bold(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(INTER_BOLD));
    }

    public static MutableComponent regular(Component text) {
        return text.copy().withStyle(Style.EMPTY.withFont(INTER));
    }
}

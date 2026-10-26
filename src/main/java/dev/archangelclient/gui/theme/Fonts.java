package dev.archangelclient.gui.theme;

import dev.archangelclient.ArchangelClient;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * The bundled Inter font, rendered through Minecraft's TTF font provider
 * (see assets/archangelclient/font/*.json).
 */
public final class Fonts {
    private Fonts() {}

    public static final FontDescription INTER = new FontDescription.Resource(ArchangelClient.id("inter"));
    public static final FontDescription INTER_BOLD = new FontDescription.Resource(ArchangelClient.id("inter_bold"));

    /** Whether vanilla's default font is currently replaced by Inter (Menu Style setting). */
    public static boolean smoothEverywhere() {
        var modules = ArchangelClient.modules();
        return modules != null && modules.get(MenuStyleModule.class).useSmoothFontEverywhere();
    }

    public static MutableComponent regular(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(INTER));
    }

    public static MutableComponent bold(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(INTER_BOLD));
    }

    public static MutableComponent regular(Component text) {
        return text.copy().withStyle(Style.EMPTY.withFont(INTER));
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static int width(String text) {
        return font().width(regular(text));
    }

    public static int widthBold(String text) {
        return font().width(bold(text));
    }

    public static void draw(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(font(), regular(text), x, y, color, false);
    }

    public static void drawBold(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(font(), bold(text), x, y, color, false);
    }

    public static void drawCentered(GuiGraphics g, String text, int cx, int y, int color) {
        draw(g, text, cx - width(text) / 2, y, color);
    }

    /** {@code text} shortened with an ellipsis so it fits in {@code maxWidth}. */
    public static String trim(String text, int maxWidth) {
        if (width(text) <= maxWidth) return text;
        String s = text;
        while (!s.isEmpty() && width(s + "…") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Word-wrapped lines of {@code text} in the regular font. */
    public static List<FormattedCharSequence> wrap(String text, int maxWidth) {
        return font().split(regular(text), maxWidth);
    }
}

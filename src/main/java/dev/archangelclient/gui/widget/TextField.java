package dev.archangelclient.gui.widget;

import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.render.RenderUtil;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

/** Single-line text input with a placeholder. Edits at the end of the text only. */
public class TextField extends UiWidget {
    private final String placeholder;
    private final int maxLength;
    private Consumer<String> onChange = s -> {};
    private String text = "";
    private boolean focused;

    public TextField(String placeholder, int maxLength) {
        this.placeholder = placeholder;
        this.maxLength = maxLength;
        this.height = 18;
    }

    public TextField onChange(Consumer<String> listener) {
        this.onChange = listener;
        return this;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        String clipped = text.length() > maxLength ? text.substring(0, maxLength) : text;
        if (clipped.equals(this.text)) return;
        this.text = clipped;
        onChange.accept(clipped);
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (focused) RenderUtil.glow(g, x, y, width, height, Theme.RADIUS, Theme.ACCENT, 0.7f);
        RenderUtil.roundedRect(g, x, y, width, height, Theme.RADIUS,
                focused ? Colors.withAlpha(Theme.ACCENT, 0x30) : (isHovered(mouseX, mouseY) ? Theme.CARD_HOVER : Theme.FIELD));
        int tx = x + 7, ty = y + (height - 8) / 2;
        int room = width - 14;
        if (text.isEmpty()) {
            Fonts.draw(g, Fonts.trim(placeholder, room), tx, ty, Theme.TEXT_DIM);
        } else {
            // show the end of the text when it's longer than the field
            String shown = text;
            while (Fonts.width(shown) > room - 3 && shown.length() > 1) shown = shown.substring(1);
            Fonts.draw(g, shown, tx, ty, Theme.TEXT);
            tx += Fonts.width(shown);
        }
        if (focused && (Util.getMillis() / 500) % 2 == 0) g.fill(tx + 1, ty - 1, tx + 2, ty + 9, Theme.ACCENT_LIGHT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        focused = isHovered(mouseX, mouseY);
        if (focused && button == 1) setText(""); // right click clears, like vanilla search boxes
        return focused;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (!focused) return false;
        boolean ctrl = (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (ctrl) setText(text.replaceAll("\\S*\\s*$", ""));
                else if (!text.isEmpty()) setText(text.substring(0, text.length() - 1));
            }
            case GLFW.GLFW_KEY_V -> {
                if (!ctrl) return true;
                setText(text + Minecraft.getInstance().keyboardHandler.getClipboard().replaceAll("[\\r\\n\\t]", ""));
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> {
                focused = false;
            }
            default -> {}
        }
        return true;
    }

    @Override
    public boolean charTyped(int codepoint) {
        if (!focused || Character.isISOControl(codepoint)) return false;
        setText(text + Character.toString(codepoint));
        return true;
    }
}

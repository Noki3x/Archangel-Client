package dev.archangelclient.gui.widget;

import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ColorSetting;
import dev.archangelclient.core.setting.KeybindSetting;
import dev.archangelclient.core.setting.ModeSetting;
import dev.archangelclient.core.setting.NumberSetting;
import dev.archangelclient.core.setting.Setting;
import dev.archangelclient.gui.theme.Fonts;
import dev.archangelclient.gui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;

/**
 * One row of a module's settings panel: name and description on the left, the control on the
 * right. {@link #of} picks the control for a setting type, which is how the panel is generated.
 */
public abstract class SettingWidget<S extends Setting<?>> extends UiWidget {
    protected final S setting;

    protected SettingWidget(S setting) {
        this.setting = setting;
        this.height = Theme.ROW_HEIGHT;
    }

    public static SettingWidget<?> of(Setting<?> setting) {
        if (setting instanceof BooleanSetting s) return new BooleanRow(s);
        if (setting instanceof NumberSetting s) return new Slider(s);
        if (setting instanceof ModeSetting s) return new Dropdown(s);
        if (setting instanceof ColorSetting s) return new ColorPicker(s);
        if (setting instanceof KeybindSetting s) return new KeybindButton(s);
        throw new IllegalArgumentException("No widget for " + setting.getClass().getSimpleName());
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }

    /** Space the label may use, leaving room for a control of {@code controlWidth} on the right. */
    protected void renderLabel(GuiGraphics g, int controlWidth) {
        int room = width - controlWidth - 12;
        Fonts.draw(g, Fonts.trim(setting.getName(), room), x, y + 4, Theme.TEXT);
        Fonts.draw(g, Fonts.trim(setting.getDescription(), room), x, y + 15, Theme.TEXT_MUTED);
    }

    /** Vertical position that centers a control of {@code h} pixels in the header line. */
    protected int controlY(int h) {
        return y + (Theme.ROW_HEIGHT - h) / 2;
    }

    private static final class BooleanRow extends SettingWidget<BooleanSetting> {
        private final Toggle toggle;

        BooleanRow(BooleanSetting setting) {
            super(setting);
            toggle = new Toggle(setting::get, setting::toggle);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            renderLabel(g, Toggle.WIDTH);
            toggle.setBounds(x + width - Toggle.WIDTH, controlY(Toggle.HEIGHT), Toggle.WIDTH, Toggle.HEIGHT);
            toggle.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // the whole row toggles, not just the switch
            if (button != 0 || !isHovered(mouseX, mouseY)) return false;
            setting.toggle();
            return true;
        }
    }
}

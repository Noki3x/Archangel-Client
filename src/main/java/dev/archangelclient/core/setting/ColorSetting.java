package dev.archangelclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** ARGB color option, saved as "#AARRGGBB". Shown as an expandable color picker. */
public class ColorSetting extends Setting<Integer> {
    private final boolean allowAlpha;

    public ColorSetting(String name, String description, int defaultArgb, boolean allowAlpha) {
        super(name, description, defaultArgb);
        this.allowAlpha = allowAlpha;
    }

    public boolean allowsAlpha() {
        return allowAlpha;
    }

    @Override
    protected Integer sanitize(Integer newValue) {
        return allowAlpha ? newValue : newValue | 0xFF000000;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(String.format("#%08X", value));
    }

    @Override
    public void fromJson(JsonElement json) {
        if (!json.isJsonPrimitive()) return;
        String s = json.getAsString().replace("#", "");
        try {
            set((int) Long.parseLong(s.length() == 6 ? "FF" + s : s, 16));
        } catch (NumberFormatException ignored) {
            // keep the current color
        }
    }
}

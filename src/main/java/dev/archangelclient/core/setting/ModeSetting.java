package dev.archangelclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;

/** One choice out of a fixed list of modes. Shown as a dropdown. */
public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = List.of(modes);
        if (!this.modes.contains(defaultValue)) throw new IllegalArgumentException(defaultValue + " is not a mode of " + name);
    }

    public List<String> getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return value.equals(mode);
    }

    public void cycle() {
        set(modes.get((modes.indexOf(value) + 1) % modes.size()));
    }

    @Override
    protected String sanitize(String newValue) {
        return modes.contains(newValue) ? newValue : value;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive()) set(json.getAsString());
    }
}

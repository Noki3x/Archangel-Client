package dev.archangelclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Free-form text value with no widget of its own. Used for state that a dedicated screen edits,
 * such as the equipped cosmetic in each slot, so it still lives in the profile.
 */
public class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String name, String description, String defaultValue, int maxLength) {
        super(name, description, defaultValue);
        this.maxLength = maxLength;
    }

    @Override
    protected String sanitize(String newValue) {
        if (newValue == null) return getDefault();
        return newValue.length() > maxLength ? newValue.substring(0, maxLength) : newValue;
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

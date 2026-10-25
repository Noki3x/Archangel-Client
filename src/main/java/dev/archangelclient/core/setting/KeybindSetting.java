package dev.archangelclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

/** A GLFW key code, or {@link #NONE}. Shown as a "press a key" capture button. */
public class KeybindSetting extends Setting<Integer> {
    public static final int NONE = InputConstants.UNKNOWN.getValue();

    public KeybindSetting(String name, String description, int defaultKey) {
        super(name, description, defaultKey);
    }

    public boolean isBound() {
        return value != NONE;
    }

    public Component keyName() {
        return isBound() ? InputConstants.Type.KEYSYM.getOrCreate(value).getDisplayName() : Component.literal("None");
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) set(json.getAsInt());
    }
}

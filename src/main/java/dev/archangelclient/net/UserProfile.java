package dev.archangelclient.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.archangelclient.cosmetics.Loadout;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * What the backend knows about an Archangel player: an optional display name and name color,
 * the badge to show next to their nametag, and their equipped cosmetics. {@link #NONE} means
 * "not an Archangel player" (or not known yet).
 */
public record UserProfile(@Nullable String displayName, @Nullable Integer nameColor, @Nullable String badge, Loadout cosmetics) {
    public static final UserProfile NONE = new UserProfile(null, null, null, Loadout.EMPTY);
    public static final String DEFAULT_BADGE = "archangel";

    public boolean isArchangelUser() {
        return this != NONE;
    }

    /**
     * Parses {@code GET /users/:uuid}:
     * {@code {"uuid", "displayName", "nameColor": "#60A5FA", "badge": "archangel", "cosmetics": {"cape": "cape_nebula"}}}.
     */
    public static UserProfile fromJson(JsonObject json) {
        String name = string(json, "displayName");
        if (name != null && !name.matches("[A-Za-z0-9_ ]{1,24}")) name = null; // plain names only, no formatting tricks
        Integer color = null;
        String c = string(json, "nameColor");
        if (c != null && c.matches("#[0-9A-Fa-f]{6}")) color = Integer.parseInt(c.substring(1), 16);
        String badge = string(json, "badge");
        if (badge != null) badge = badge.toLowerCase(Locale.ROOT);
        Loadout cosmetics = json.has("cosmetics") && json.get("cosmetics").isJsonObject()
                ? Loadout.fromJson(json.getAsJsonObject("cosmetics")) : Loadout.EMPTY;
        return new UserProfile(name, color, badge == null ? DEFAULT_BADGE : badge, cosmetics);
    }

    /** A local-only profile for yourself while the backend is unreachable. */
    public static UserProfile offlineSelf(Loadout cosmetics) {
        return new UserProfile(null, null, DEFAULT_BADGE, cosmetics);
    }

    private static @Nullable String string(JsonObject json, String key) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }
}

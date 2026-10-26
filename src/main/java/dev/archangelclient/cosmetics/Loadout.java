package dev.archangelclient.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** What a player has equipped: at most one cosmetic id per slot. Immutable. */
public final class Loadout {
    public static final Loadout EMPTY = new Loadout(new EnumMap<>(CosmeticType.class));

    private final Map<CosmeticType, String> equipped;

    private Loadout(EnumMap<CosmeticType, String> equipped) {
        this.equipped = Collections.unmodifiableMap(equipped);
    }

    public static Loadout of(Map<CosmeticType, String> equipped) {
        EnumMap<CosmeticType, String> copy = new EnumMap<>(CosmeticType.class);
        equipped.forEach((type, id) -> {
            if (id != null && !id.isBlank()) copy.put(type, id);
        });
        return copy.isEmpty() ? EMPTY : new Loadout(copy);
    }

    /** Reads {@code {"cape": "cape_nebula", ...}}; unknown slots are ignored. */
    public static Loadout fromJson(JsonObject json) {
        EnumMap<CosmeticType, String> map = new EnumMap<>(CosmeticType.class);
        for (Map.Entry<String, JsonElement> e : json.entrySet()) {
            CosmeticType type = CosmeticType.byKey(e.getKey());
            if (type != null && e.getValue().isJsonPrimitive()) map.put(type, e.getValue().getAsString());
        }
        return of(map);
    }

    public String get(CosmeticType type) {
        return equipped.get(type);
    }

    public boolean isEmpty() {
        return equipped.isEmpty();
    }

    public Loadout with(CosmeticType type, String id) {
        EnumMap<CosmeticType, String> copy = new EnumMap<>(CosmeticType.class);
        copy.putAll(equipped);
        if (id == null) copy.remove(type);
        else copy.put(type, id);
        return of(copy);
    }

    public Map<CosmeticType, String> asMap() {
        return equipped;
    }
}

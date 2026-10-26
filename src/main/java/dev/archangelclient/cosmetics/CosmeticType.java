package dev.archangelclient.cosmetics;

import java.util.Locale;

/** Equip slots. One cosmetic of each type can be worn at a time. */
public enum CosmeticType {
    CAPE("Capes", "Cape"),
    WINGS("Wings", "Wings"),
    HAT("Hats", "Hat"),
    HALO("Halos", "Halo"),
    AURA("Auras", "Aura"),
    TRAIL("Swing trails", "Swing trail"),
    HIT_EFFECT("Hit effects", "Hit effect");

    private final String plural;
    private final String singular;

    CosmeticType(String plural, String singular) {
        this.plural = plural;
        this.singular = singular;
    }

    public String plural() {
        return plural;
    }

    public String singular() {
        return singular;
    }

    /** Name used in the catalog and in backend payloads, e.g. {@code hit_effect}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CosmeticType byKey(String key) {
        for (CosmeticType t : values()) if (t.key().equals(key)) return t;
        return null;
    }
}

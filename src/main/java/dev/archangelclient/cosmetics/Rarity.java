package dev.archangelclient.cosmetics;

import java.util.Locale;

/** Display tier of a cosmetic; only affects the color of its card. */
public enum Rarity {
    COMMON("Common", 0xFF9CA0AB),
    RARE("Rare", 0xFF60A5FA),
    EPIC("Epic", 0xFF8B5CF6),
    LEGENDARY("Legendary", 0xFFF5C46B);

    private final String displayName;
    private final int color;

    Rarity(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public int color() {
        return color;
    }

    public static Rarity byKey(String key) {
        try {
            return valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}

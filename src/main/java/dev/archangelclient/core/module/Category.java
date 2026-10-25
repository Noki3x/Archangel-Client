package dev.archangelclient.core.module;

/** Sidebar groups of the client settings screen. Profiles is a separate page, not a category. */
public enum Category {
    HUD("HUD"),
    ANIMATIONS("Animations"),
    COSMETICS("Cosmetics"),
    EMOTES("Emotes"),
    VISUALS("Visuals"),
    NAMETAGS("Nametags"),
    PERFORMANCE("Performance");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

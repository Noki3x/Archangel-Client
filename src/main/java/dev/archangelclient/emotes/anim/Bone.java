package dev.archangelclient.emotes.anim;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * The player rig an emote can animate. {@code ROOT} moves and turns the whole player (armor, cape
 * and cosmetics included) around the feet; the others are the vanilla model parts.
 */
public enum Bone {
    ROOT, HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG;

    /** Accepts Java ({@code right_arm}) and Bedrock ({@code rightArm}) spellings. */
    public static @Nullable Bone byName(String name) {
        String key = name.replace("_", "").toLowerCase(Locale.ROOT);
        for (Bone bone : values()) if (bone.name().replace("_", "").toLowerCase(Locale.ROOT).equals(key)) return bone;
        return switch (key) {
            case "waist", "player" -> ROOT;
            default -> null;
        };
    }
}

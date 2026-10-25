package dev.archangelclient.gui.widget;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerSkinWidget;

/**
 * Live 3D preview of the signed-in player's skin, fetched through the session's game profile.
 * Built on the vanilla {@link PlayerSkinWidget} (drag to rotate); the slow idle spin and
 * mouse-follow are layered on top in milestone 10.
 */
public final class PlayerPreview {
    private PlayerPreview() {}

    public static PlayerSkinWidget create(int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        GameProfile profile = mc.getGameProfile();
        // Falls back to the default skin until the session's skin has downloaded (or when offline).
        var skin = mc.getSkinManager().createLookup(profile, false);
        return new PlayerSkinWidget(width, height, mc.getEntityModels(), skin);
    }
}

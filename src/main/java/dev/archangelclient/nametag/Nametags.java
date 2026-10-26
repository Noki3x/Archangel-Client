package dev.archangelclient.nametag;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.net.UserProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Builds player nametags: the wings badge for Archangel players, and their display name and color
 * from the backend. Server formatting wins: a player on a scoreboard team (ranks, team colors)
 * keeps the server's name and only gets the badge. A display name that differs from the account
 * name always shows the real name after it, so nobody can pass as someone else.
 */
public final class Nametags {
    /** The badge is one glyph in a bitmap font (assets/archangelclient/font/badge.json). */
    private static final Component BADGE = Component.literal("")
            .withStyle(Style.EMPTY.withFont(new FontDescription.Resource(ArchangelClient.id("badge"))).withColor(ChatFormatting.WHITE));

    private Nametags() {}

    private static NametagsModule module() {
        return ArchangelClient.modules().get(NametagsModule.class);
    }

    /** The profile to show for {@code player}; yourself always counts as an Archangel player. */
    public static UserProfile profileOf(AbstractClientPlayer player) {
        UserProfile profile = ArchangelClient.profiles().get(player.getUUID());
        if (player == Minecraft.getInstance().player && !profile.isArchangelUser()) {
            return UserProfile.offlineSelf(ArchangelClient.cosmetics().localLoadout());
        }
        return profile;
    }

    public static Component decorate(AbstractClientPlayer player, Component vanilla) {
        NametagsModule module = module();
        if (!module.isEnabled()) return vanilla;
        UserProfile profile = profileOf(player);
        if (!profile.isArchangelUser()) return vanilla;

        Component name = vanilla;
        if (module.customNames.get() && player.getTeam() == null && (profile.displayName() != null || profile.nameColor() != null)) {
            name = styledName(player.getName().getString(), profile);
        }
        if (!module.badges.get()) return name;
        return Component.empty().append(BADGE).append(" ").append(name);
    }

    private static Component styledName(String accountName, UserProfile profile) {
        String shown = profile.displayName() != null ? profile.displayName() : accountName;
        MutableComponent name = Component.literal(shown);
        if (profile.nameColor() != null) name.withColor(profile.nameColor());
        if (!shown.equalsIgnoreCase(accountName)) {
            name.append(Component.literal(" (" + accountName + ")").withStyle(ChatFormatting.GRAY));
        }
        return name;
    }

    /** Nametag background alpha (0..255), or -1 to keep vanilla's. */
    public static int backgroundAlpha() {
        NametagsModule module = module();
        return module.isEnabled() ? (int) Math.round(module.backgroundOpacity.get() / 100.0 * 255) : -1;
    }

    public static float scale() {
        NametagsModule module = module();
        return module.isEnabled() ? module.scale.get().floatValue() : 1f;
    }

    public static boolean shadow() {
        NametagsModule module = module();
        return module.isEnabled() && module.textShadow.get();
    }

    /** Whether to show your own nametag (third person only). */
    public static boolean showOwn() {
        NametagsModule module = module();
        Minecraft mc = Minecraft.getInstance();
        return module.isEnabled() && module.showOwn.get() && !mc.options.getCameraType().isFirstPerson() && Minecraft.renderNames();
    }
}

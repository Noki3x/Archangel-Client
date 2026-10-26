package dev.archangelclient.emotes;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;

/**
 * Emotes on/off and their options. The wheel opens with the "Emote wheel" key (B by default,
 * rebindable in vanilla Controls). Emotes are visual only: nothing is sent to the game server.
 */
public class EmotesModule extends Module {
    public final BooleanSetting thirdPerson = add(new BooleanSetting("Third person while emoting",
            "Switch to the third-person camera while you emote, and back afterwards", true));
    public final BooleanSetting showOthers = add(new BooleanSetting("Show other players' emotes",
            "Play emotes other Archangel players start", true));
    public final BooleanSetting share = add(new BooleanSetting("Share my emotes",
            "Let other Archangel players see your emotes", true));

    public EmotesModule() {
        super("Emotes", "Emote wheel with wave, dab, clap, floss, sit, bow and more", Category.EMOTES, true);
    }

    @Override
    protected void onDisable() {
        ArchangelClient.emotes().stopLocal();
    }
}

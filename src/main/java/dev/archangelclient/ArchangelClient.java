package dev.archangelclient;

import dev.archangelclient.gui.render.WindowIcon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Archangel Client entrypoint.
 *
 * <p>Archangel is a legit client: every feature is cosmetic, a UI/HUD feature or a performance
 * feature. Nothing here gives a gameplay advantage or reveals information the vanilla client hides.
 */
public final class ArchangelClient implements ClientModInitializer {
    public static final String MOD_ID = "archangelclient";
    public static final String NAME = "Archangel Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", NAME);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> WindowIcon.apply(client.getWindow()));
    }

    /** Shorthand for a resource location in the mod's namespace. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

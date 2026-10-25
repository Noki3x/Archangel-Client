package dev.archangelclient;

import com.mojang.blaze3d.platform.InputConstants;
import dev.archangelclient.core.config.ConfigManager;
import dev.archangelclient.core.event.AttackEntityEvent;
import dev.archangelclient.core.event.EventBus;
import dev.archangelclient.core.event.Render2DEvent;
import dev.archangelclient.core.event.Render3DEvent;
import dev.archangelclient.core.event.TickEvent;
import dev.archangelclient.core.module.ModuleManager;
import dev.archangelclient.gui.render.WindowIcon;
import dev.archangelclient.gui.screen.ClientSettingsScreen;
import dev.archangelclient.gui.theme.MenuStyleModule;
import dev.archangelclient.hud.FpsModule;
import dev.archangelclient.performance.UnfocusedFpsModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;
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

    private static EventBus events;
    private static ModuleManager modules;
    private static ConfigManager config;
    private static KeyMapping openSettingsKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", NAME);
        events = new EventBus();
        modules = new ModuleManager(events, () -> config.markDirty());
        config = new ConfigManager(modules);

        modules.register(new FpsModule());
        modules.register(new MenuStyleModule());
        modules.register(new UnfocusedFpsModule());
        config.init();

        var category = KeyMapping.Category.register(id("main"));
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.archangelclient.settings",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        registerHooks();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> WindowIcon.apply(client.getWindow()));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());
    }

    /** Bridges Fabric API callbacks onto the client's own event bus. */
    private static void registerHooks() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettingsKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new ClientSettingsScreen(null));
            }
            events.post(TickEvent.INSTANCE);
            config.tick();
        });
        HudElementRegistry.addLast(id("hud"), (graphics, delta) ->
                events.post(new Render2DEvent(graphics, delta.getGameTimeDeltaPartialTick(false))));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> events.post(new Render3DEvent(context.matrices(),
                context.consumers(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false))));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            // display-only: observe the local player's own attacks, never alter or cancel them
            if (level.isClientSide()) events.post(new AttackEntityEvent(entity));
            return InteractionResult.PASS;
        });
    }

    public static EventBus events() {
        return events;
    }

    public static ModuleManager modules() {
        return modules;
    }

    public static ConfigManager config() {
        return config;
    }

    /** False until the client has finished initializing (mixins can run earlier than that). */
    public static boolean isReady() {
        return config != null;
    }

    /** Shorthand for a resource location in the mod's namespace. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

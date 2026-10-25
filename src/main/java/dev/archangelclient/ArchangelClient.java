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
import dev.archangelclient.gui.screen.HudEditorScreen;
import dev.archangelclient.hud.ClickCounter;
import dev.archangelclient.hud.CombatTracker;
import dev.archangelclient.hud.HudManager;
import dev.archangelclient.hud.element.ArmorElement;
import dev.archangelclient.hud.element.ClockElement;
import dev.archangelclient.hud.element.ComboElement;
import dev.archangelclient.hud.element.CoordinatesElement;
import dev.archangelclient.hud.element.CpsElement;
import dev.archangelclient.hud.element.FpsElement;
import dev.archangelclient.hud.element.KeystrokesElement;
import dev.archangelclient.hud.element.MemoryElement;
import dev.archangelclient.hud.element.PingElement;
import dev.archangelclient.hud.element.PotionsElement;
import dev.archangelclient.hud.element.ReachElement;
import dev.archangelclient.hud.element.ServerElement;
import dev.archangelclient.hud.element.SpeedElement;
import dev.archangelclient.hud.element.TargetHudElement;
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
    private static HudManager hud;
    private static CombatTracker combat;
    private static ClickCounter clicks;
    private static KeyMapping openSettingsKey;
    private static KeyMapping openHudEditorKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", NAME);
        events = new EventBus();
        modules = new ModuleManager(events, () -> config.markDirty());
        config = new ConfigManager(modules);

        hud = new HudManager(modules);
        combat = new CombatTracker(events);
        clicks = new ClickCounter(events);

        modules.register(new FpsElement());
        modules.register(new CpsElement());
        modules.register(new KeystrokesElement());
        modules.register(new PingElement());
        modules.register(new CoordinatesElement());
        modules.register(new ArmorElement());
        modules.register(new PotionsElement());
        modules.register(new ClockElement());
        modules.register(new SpeedElement());
        modules.register(new ServerElement());
        modules.register(new MemoryElement());
        modules.register(new ComboElement());
        modules.register(new ReachElement());
        modules.register(new TargetHudElement());
        modules.register(new MenuStyleModule());
        modules.register(new UnfocusedFpsModule());
        config.init();

        var category = KeyMapping.Category.register(id("main"));
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.archangelclient.settings",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        openHudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.archangelclient.hud_editor",
                InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));

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
            while (openHudEditorKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new HudEditorScreen(null));
            }
            events.post(TickEvent.INSTANCE);
            config.tick();
        });
        HudElementRegistry.addLast(id("hud"), (graphics, delta) -> {
            float partialTick = delta.getGameTimeDeltaPartialTick(false);
            hud.render(graphics, partialTick);
            events.post(new Render2DEvent(graphics, partialTick));
        });
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

    public static HudManager hud() {
        return hud;
    }

    public static CombatTracker combat() {
        return combat;
    }

    public static ClickCounter clicks() {
        return clicks;
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

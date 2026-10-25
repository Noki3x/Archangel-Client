package dev.archangelclient.core.module;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.event.EventBus;
import dev.archangelclient.core.setting.KeybindSetting;
import dev.archangelclient.core.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A client feature. Event handlers declared with {@link #on} are only subscribed while the module
 * is enabled, so a disabled module costs nothing per frame.
 *
 * <p>Every module must be cosmetic, a UI/HUD feature or a performance feature.
 */
public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final KeybindSetting keybind;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Handler<?>> handlers = new ArrayList<>();
    private final List<EventBus.Subscription> subscriptions = new ArrayList<>();
    private final boolean enabledByDefault;
    private Runnable changeListener = () -> {};
    private boolean enabled;

    protected Module(String name, String description, Category category, boolean enabledByDefault) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabledByDefault = enabledByDefault;
        this.keybind = new KeybindSetting("Keybind", "Toggles " + name + " in game", KeybindSetting.NONE);
    }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    /** Declares an event handler that is active while the module is enabled. */
    protected <T> void on(Class<T> type, Consumer<? super T> handler) {
        handlers.add(new Handler<>(type, handler));
    }

    protected void onEnable() {}

    protected void onDisable() {}

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        EventBus bus = ArchangelClient.events();
        if (enabled) {
            handlers.forEach(h -> subscriptions.add(h.subscribe(bus)));
            onEnable();
        } else {
            subscriptions.forEach(EventBus.Subscription::unsubscribe);
            subscriptions.clear();
            onDisable();
        }
        changeListener.run();
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public KeybindSetting getKeybind() {
        return keybind;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /** Called by the module manager so state changes trigger a config auto-save. */
    void setChangeListener(Runnable listener) {
        this.changeListener = listener;
        keybind.onChange(listener);
        settings.forEach(s -> s.onChange(listener));
    }

    private record Handler<T>(Class<T> type, Consumer<? super T> handler) {
        EventBus.Subscription subscribe(EventBus bus) {
            return bus.subscribe(type, handler);
        }
    }
}

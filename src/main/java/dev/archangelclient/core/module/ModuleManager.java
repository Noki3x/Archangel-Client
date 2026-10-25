package dev.archangelclient.core.module;

import dev.archangelclient.core.event.EventBus;
import dev.archangelclient.core.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;

/** Owns every module, and toggles modules from their keybinds while playing. */
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Runnable changeListener;

    public ModuleManager(EventBus bus, Runnable changeListener) {
        this.changeListener = changeListener;
        bus.subscribe(KeyEvent.class, this::onKey);
    }

    public <M extends Module> M register(M module) {
        modules.add(module);
        module.setChangeListener(changeListener);
        return module;
    }

    public List<Module> all() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> byCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }

    /** Modules whose name or description contains {@code query}, ignoring case. */
    public List<Module> search(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        return modules.stream()
                .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains(q)
                        || m.getDescription().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    public Optional<Module> byName(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
    }

    public <M extends Module> M get(Class<M> type) {
        for (Module m : modules) if (type.isInstance(m)) return type.cast(m);
        throw new IllegalArgumentException(type.getSimpleName() + " is not registered");
    }

    private void onKey(KeyEvent event) {
        // only while playing, so typing in chat or a menu never toggles anything
        if (!event.isPress() || Minecraft.getInstance().screen != null) return;
        for (Module m : modules) {
            if (m.getKeybind().isBound() && m.getKeybind().get() == event.key()) m.toggle();
        }
    }
}

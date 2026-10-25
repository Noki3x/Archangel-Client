package dev.archangelclient.core.setting;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * A named, persisted module option. The settings GUI is generated from these: each subclass maps
 * to one widget, and {@link #visibleWhen} hides options that don't apply to the current state.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    private final List<Runnable> listeners = new ArrayList<>();
    private BooleanSupplier visible = () -> true;
    protected T value;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        T sanitized = sanitize(newValue);
        if (Objects.equals(value, sanitized)) return;
        value = sanitized;
        listeners.forEach(Runnable::run);
    }

    public T getDefault() {
        return defaultValue;
    }

    public void reset() {
        set(defaultValue);
    }

    /** Clamps or otherwise corrects a value before it's stored. */
    protected T sanitize(T newValue) {
        return newValue;
    }

    public void onChange(Runnable listener) {
        listeners.add(listener);
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
        this.visible = condition;
        return (S) this;
    }

    public boolean isVisible() {
        return visible.getAsBoolean();
    }

    public abstract JsonElement toJson();

    /** Loads a saved value; malformed values are ignored and the current value is kept. */
    public abstract void fromJson(JsonElement json);
}

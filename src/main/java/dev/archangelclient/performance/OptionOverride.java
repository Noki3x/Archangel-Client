package dev.archangelclient.performance;

import java.util.Objects;
import net.minecraft.client.OptionInstance;
import org.jspecify.annotations.Nullable;

/**
 * Holds a vanilla option at a module's value while the module is on, and puts the player's own
 * value back when it's turned off. Values always go through {@link OptionInstance#set}, so they
 * stay inside vanilla's limits.
 */
public final class OptionOverride<T> {
    private @Nullable OptionInstance<T> option;
    private @Nullable T original;

    /** Sets {@code option} to {@code value}, remembering what the player had the first time. */
    public void apply(OptionInstance<T> option, T value) {
        if (this.option == null) {
            this.option = option;
            this.original = option.get();
        }
        if (!Objects.equals(option.get(), value)) option.set(value);
    }

    /** Restores the remembered value, if this override was ever applied. */
    public void restore() {
        if (option != null && original != null) option.set(original);
        option = null;
        original = null;
    }
}

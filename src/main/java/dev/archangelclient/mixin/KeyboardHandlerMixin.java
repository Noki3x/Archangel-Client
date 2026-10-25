package dev.archangelclient.mixin;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.event.KeyEvent;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    /** Forwards raw key presses to the event bus (for module keybinds). Never cancels vanilla handling. */
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void archangel$onKey(long window, int action, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
        ArchangelClient.events().post(new KeyEvent(event.key(), event.scancode(), action, event.modifiers()));
    }
}

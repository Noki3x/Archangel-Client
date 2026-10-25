package dev.archangelclient.mixin;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.event.MouseClickEvent;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    /** Forwards raw mouse buttons to the event bus (CPS counter, keystrokes). Never cancels input. */
    @Inject(method = "onButton", at = @At("HEAD"))
    private void archangel$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (ArchangelClient.isReady()) ArchangelClient.events().post(new MouseClickEvent(info.button(), action));
    }
}

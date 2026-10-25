package dev.archangelclient.mixin;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.gui.screen.ArchangelTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** Every request for the vanilla title screen opens the Archangel one instead. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen archangel$replaceTitleScreen(Screen screen) {
        return screen instanceof TitleScreen ? new ArchangelTitleScreen() : screen;
    }

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void archangel$windowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ArchangelClient.NAME + " · " + cir.getReturnValue());
    }
}

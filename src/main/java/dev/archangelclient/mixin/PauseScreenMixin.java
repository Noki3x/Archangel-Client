package dev.archangelclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.archangelclient.gui.screen.ClientSettingsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a full-width "Client Settings" button to the pause menu, just above Disconnect. */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    /** Same width vanilla uses for its full-width pause menu buttons. */
    private static final int FULL_ROW_WIDTH = 204;

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/chat/CommonComponents;disconnectButtonLabel(Z)Lnet/minecraft/network/chat/Component;"))
    private void archangel$addClientSettings(CallbackInfo ci, @Local GridLayout.RowHelper rows) {
        Screen pause = this;
        rows.addChild(Button.builder(Component.translatable("archangelclient.pause.client_settings"),
                        button -> minecraft.setScreen(new ClientSettingsScreen(pause)))
                .width(FULL_ROW_WIDTH).build(), 2);
    }
}

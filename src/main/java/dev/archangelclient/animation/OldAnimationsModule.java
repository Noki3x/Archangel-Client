package dev.archangelclient.animation;

import dev.archangelclient.core.event.MouseClickEvent;
import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

/**
 * 1.7-style first-person animations. Purely visual: nothing here changes what is sent to the
 * server. The blockhit pose is drawn while you hold use with a sword (modern swords can't block,
 * so it's just the look), and "swing while using" plays a local-only swing that sends no packet.
 */
public class OldAnimationsModule extends Module {
    public final BooleanSetting blockhit = add(new BooleanSetting("1.7 blockhit", "Hold use with a sword to show the old block pose, and swing while in it", true));
    public final BooleanSetting oldSwing = add(new BooleanSetting("Old swing", "Your item doesn't dip down after each hit (no attack cooldown animation)", true));
    public final BooleanSetting oldEat = add(new BooleanSetting("Old eat & drink", "1.7 eating and drinking, with the swing showing while you eat", true));
    public final BooleanSetting oldBow = add(new BooleanSetting("Old bow", "1.7 bow draw, with the swing showing while you draw", true));
    public final BooleanSetting swingWhileUsing = add(new BooleanSetting("Swing while using", "Clicking while eating, drinking or drawing plays the swing (only on your screen)", true));
    public final BooleanSetting thirdPerson = add(new BooleanSetting("Third-person blocking", "Show the blocking arm pose on your own player in third person", true));

    public OldAnimationsModule() {
        super("1.7 Animations", "Old blockhit, swing, eating and bow animations", Category.ANIMATIONS, true);
        on(MouseClickEvent.class, this::onClick);
    }

    /** True while the blockhit pose should show: sword in hand, use held, not using any other item. */
    public boolean isBlockhitting() {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        return isEnabled() && blockhit.get() && player != null && mc.screen == null
                && mc.options.keyUse.isDown() && !player.isUsingItem()
                && player.getMainHandItem().is(ItemTags.SWORDS);
    }

    private void onClick(MouseClickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (!swingWhileUsing.get() || player == null || mc.screen != null) return;
        if (event.isPress() && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && player.isUsingItem()) {
            // LivingEntity.swing(hand, false) only animates locally; LocalPlayer.swing(hand) would notify the server
            player.swing(InteractionHand.MAIN_HAND, false);
        }
    }
}

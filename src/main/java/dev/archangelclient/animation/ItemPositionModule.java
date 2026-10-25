package dev.archangelclient.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.NumberSetting;
import net.minecraft.world.entity.HumanoidArm;

/** Moves, rotates and scales the item held in first person. Mirrored for the left hand. */
public class ItemPositionModule extends Module {
    private final NumberSetting x = add(new NumberSetting("X offset", "Left / right", 0, -1, 1, 0.05));
    private final NumberSetting y = add(new NumberSetting("Y offset", "Down / up", 0, -1, 1, 0.05));
    private final NumberSetting z = add(new NumberSetting("Z offset", "Closer / further", 0, -1, 1, 0.05));
    private final NumberSetting scale = add(new NumberSetting("Scale", "Size of the held item", 1, 0.25, 2, 0.05).suffix("x"));
    private final NumberSetting rotX = add(new NumberSetting("Rotation X", "Tilt forward / back", 0, -180, 180, 5).suffix("°"));
    private final NumberSetting rotY = add(new NumberSetting("Rotation Y", "Turn left / right", 0, -180, 180, 5).suffix("°"));
    private final NumberSetting rotZ = add(new NumberSetting("Rotation Z", "Roll", 0, -180, 180, 5).suffix("°"));

    public ItemPositionModule() {
        super("Item Position", "Adjust where your held item sits in first person", Category.ANIMATIONS, false);
    }

    public void apply(PoseStack pose, HumanoidArm arm) {
        if (!isEnabled()) return;
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(side * x.getFloat(), y.getFloat(), -z.getFloat());
        pose.mulPose(Axis.XP.rotationDegrees(rotX.getFloat()));
        pose.mulPose(Axis.YP.rotationDegrees(side * rotY.getFloat()));
        pose.mulPose(Axis.ZP.rotationDegrees(side * rotZ.getFloat()));
        float s = scale.getFloat();
        pose.scale(s, s, s);
    }
}

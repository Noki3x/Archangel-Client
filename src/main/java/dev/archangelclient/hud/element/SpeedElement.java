package dev.archangelclient.hud.element;

import dev.archangelclient.core.event.TickEvent;
import dev.archangelclient.hud.TextHudElement;
import java.util.Locale;
import net.minecraft.world.phys.Vec3;

/** Horizontal movement speed in blocks per second, smoothed over a few ticks. */
public class SpeedElement extends TextHudElement {
    private Vec3 lastPos;
    private double speed;

    public SpeedElement() {
        super("Speed", "Shows your movement speed in blocks per second", 0f, 0.30f, false);
        on(TickEvent.class, e -> tick());
    }

    private void tick() {
        var player = mc().player;
        if (player == null) {
            lastPos = null;
            return;
        }
        Vec3 pos = player.position();
        if (lastPos != null) {
            double dx = pos.x - lastPos.x, dz = pos.z - lastPos.z;
            double current = Math.sqrt(dx * dx + dz * dz) * 20;
            speed += (current - speed) * 0.35;
        }
        lastPos = pos;
    }

    @Override
    protected String text(boolean editor) {
        return String.format(Locale.ROOT, "%.2f b/s", speed);
    }
}

package dev.archangelclient.cosmetics.effect;

import java.util.ArrayDeque;
import java.util.Iterator;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** Recent positions of the local player, sampled every tick, for aura afterimages. */
public final class Afterimages {
    /** Ticks between afterimages. */
    public static final int SPACING = 2;
    private static final int CAPACITY = 8 * SPACING + 2;
    private static final double TELEPORT_DISTANCE_SQ = 8 * 8;

    public record Sample(double x, double y, double z, float bodyRot) {}

    private final ArrayDeque<Sample> samples = new ArrayDeque<>();

    public void tick(@Nullable Player player) {
        if (player == null) {
            samples.clear();
            return;
        }
        Sample last = samples.peekFirst();
        if (last != null && player.distanceToSqr(last.x(), last.y(), last.z()) > TELEPORT_DISTANCE_SQ) samples.clear();
        samples.addFirst(new Sample(player.getX(), player.getY(), player.getZ(), player.yBodyRot));
        while (samples.size() > CAPACITY) samples.removeLast();
    }

    /** The sample {@code ticksBack} ticks ago, or null if there isn't one yet. */
    public @Nullable Sample get(int ticksBack) {
        if (ticksBack >= samples.size()) return null;
        Iterator<Sample> it = samples.iterator();
        for (int i = 0; i < ticksBack; i++) it.next();
        return it.next();
    }
}

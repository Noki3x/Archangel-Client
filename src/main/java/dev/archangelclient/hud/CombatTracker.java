package dev.archangelclient.hud;

import dev.archangelclient.core.event.AttackEntityEvent;
import dev.archangelclient.core.event.EventBus;
import dev.archangelclient.core.event.TickEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Combat stats for display (combo, reach, target HUD), built only from your own attacks through
 * the vanilla attack path and from data the vanilla client already has about the entity you hit.
 */
public final class CombatTracker {
    public static final long COMBO_TIMEOUT_MS = 3000;
    private static final int MAX_SAMPLES = 12;

    private LivingEntity target;
    private long lastAttackAt;
    private float lastTargetHealth;
    private final Deque<Float> damageSamples = new ArrayDeque<>();
    private double lastReach = -1;
    private int combo;
    private long lastComboHitAt;
    private int lastHurtTime;

    public CombatTracker(EventBus bus) {
        bus.subscribe(AttackEntityEvent.class, this::onAttack);
        bus.subscribe(TickEvent.class, e -> tick());
    }

    private void onAttack(AttackEntityEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(event.target() instanceof LivingEntity living) || living == player) return;
        long now = Util.getMillis();
        if (living != target) {
            target = living;
            damageSamples.clear();
            lastTargetHealth = living.getHealth();
        }
        lastAttackAt = now;
        lastReach = distanceToBox(player.getEyePosition(), living.getBoundingBox());
        // a target still flashing red is in its damage cooldown, so that swing can't land
        if (living.hurtTime <= 0) {
            combo = now - lastComboHitAt > COMBO_TIMEOUT_MS ? 1 : combo + 1;
            lastComboHitAt = now;
        }
    }

    private void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            target = null;
            combo = 0;
            return;
        }
        // getting hit ends your combo
        if (player.hurtTime > lastHurtTime) combo = 0;
        lastHurtTime = player.hurtTime;
        if (combo > 0 && Util.getMillis() - lastComboHitAt > COMBO_TIMEOUT_MS) combo = 0;

        if (target != null) {
            float health = target.getHealth();
            if (health < lastTargetHealth) {
                damageSamples.addLast(lastTargetHealth - health);
                while (damageSamples.size() > MAX_SAMPLES) damageSamples.pollFirst();
            }
            lastTargetHealth = health;
            if (target.isRemoved()) target = null;
        }
    }

    private static double distanceToBox(Vec3 eye, AABB box) {
        double x = Math.max(box.minX, Math.min(eye.x, box.maxX));
        double y = Math.max(box.minY, Math.min(eye.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(eye.z, box.maxZ));
        return eye.distanceTo(new Vec3(x, y, z));
    }

    public LivingEntity getTarget() {
        return target;
    }

    public long msSinceAttack() {
        return Util.getMillis() - lastAttackAt;
    }

    public Deque<Float> getDamageSamples() {
        return damageSamples;
    }

    /** Distance from your eyes to the hitbox of the last entity you hit, or -1 if none yet. */
    public double getLastReach() {
        return lastReach;
    }

    public int getCombo() {
        return combo;
    }
}

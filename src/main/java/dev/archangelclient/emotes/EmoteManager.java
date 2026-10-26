package dev.archangelclient.emotes;

import dev.archangelclient.ArchangelClient;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Who is emoting. Your own emote switches the camera to third person (if enabled) and is
 * cancelled by any movement input or by being moved; other players' emotes arrive through
 * {@link EmoteSync} and stop when they move.
 */
public final class EmoteManager {
    /** How far (blocks, horizontally) a player may drift before their emote is cancelled. */
    private static final double MOVE_CANCEL = 0.08;

    private final EmoteRegistry registry = new EmoteRegistry();
    private final Map<UUID, EmotePlayback> playing = new HashMap<>();
    private final Map<UUID, Vec3> anchors = new HashMap<>();
    private final EmoteSync sync = new EmoteSync(this);
    private @Nullable CameraType cameraBefore;

    public EmoteRegistry registry() {
        return registry;
    }

    public EmoteSync sync() {
        return sync;
    }

    private static EmotesModule module() {
        return ArchangelClient.modules().get(EmotesModule.class);
    }

    public @Nullable EmotePlayback playing(UUID uuid) {
        return playing.get(uuid);
    }

    /** Starts an emote on your own player. Returns false if you can't emote right now. */
    public boolean playLocal(Emote emote) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !module().isEnabled() || !canEmote(player)) return false;
        start(player, emote);
        if (module().thirdPerson.get() && mc.options.getCameraType().isFirstPerson()) {
            if (cameraBefore == null) cameraBefore = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (module().share.get()) sync.sendStart(emote);
        return true;
    }

    /** Stops your own emote (blending out) and restores the camera. */
    public void stopLocal() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        EmotePlayback p = playing.get(player.getUUID());
        if (p != null && !p.stopping()) {
            p.stop(Util.getMillis());
            sync.sendStop();
        }
        restoreCamera();
    }

    /** A remote player started an emote (already validated by {@link EmoteSync}). */
    void playRemote(UUID uuid, Emote emote) {
        if (!module().isEnabled() || !module().showOthers.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getPlayerByUUID(uuid) instanceof AbstractClientPlayer player)) return;
        if (player == mc.player || !canEmote(player)) return;
        start(player, emote);
    }

    void stopRemote(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && uuid.equals(mc.player.getUUID())) return;
        EmotePlayback p = playing.get(uuid);
        if (p != null) p.stop(Util.getMillis());
    }

    private void start(Player player, Emote emote) {
        playing.put(player.getUUID(), new EmotePlayback(emote, Util.getMillis()));
        anchors.put(player.getUUID(), player.position());
    }

    private static boolean canEmote(Player player) {
        return player.isAlive() && !player.isPassenger() && !player.isSleeping() && !player.isFallFlying()
                && !player.isSwimming() && !player.isSpectator();
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            clear();
            return;
        }
        long now = Util.getMillis();
        Iterator<Map.Entry<UUID, EmotePlayback>> it = playing.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID uuid = entry.getKey();
            EmotePlayback p = entry.getValue();
            Player player = mc.level.getPlayerByUUID(uuid);
            boolean self = player != null && player == mc.player;
            if (p.done(now) || player == null) {
                it.remove();
                anchors.remove(uuid);
                if (self) {
                    if (!p.stopping()) sync.sendStop();
                    restoreCamera();
                }
                continue;
            }
            if (p.stopping()) continue;
            Vec3 anchor = anchors.get(uuid);
            boolean moved = anchor != null && player.position().subtract(anchor).horizontalDistanceSqr() > MOVE_CANCEL * MOVE_CANCEL;
            if (moved || !canEmote(player) || (self && hasMovementInput((LocalPlayer) player))) {
                if (self) stopLocal();
                else p.stop(now);
            }
        }
    }

    private static boolean hasMovementInput(LocalPlayer player) {
        Input in = player.input.keyPresses;
        return in.forward() || in.backward() || in.left() || in.right() || in.jump() || in.shift();
    }

    private void restoreCamera() {
        Minecraft mc = Minecraft.getInstance();
        // leave the camera alone if the player changed it themselves meanwhile
        if (cameraBefore != null && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(cameraBefore);
        }
        cameraBefore = null;
    }

    public void clear() {
        playing.clear();
        anchors.clear();
        cameraBefore = null;
    }

    /** The pose to draw for {@code player} this frame, or null if they aren't emoting. */
    public @Nullable EmotePose poseFor(AbstractClientPlayer player) {
        if (playing.isEmpty()) return null;
        EmotePlayback p = playing.get(player.getUUID());
        if (p == null) return null;
        long now = Util.getMillis();
        float weight = p.weight(now);
        if (weight <= 0) return null;
        var animation = p.emote().animation();
        return EmotePose.sample(animation, animation.timeAt(p.elapsed(now)), weight);
    }
}

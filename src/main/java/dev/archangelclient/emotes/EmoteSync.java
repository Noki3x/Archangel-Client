package dev.archangelclient.emotes;

import com.google.gson.JsonObject;
import dev.archangelclient.net.BackendSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/**
 * Shares emotes with other Archangel players over the backend socket.
 *
 * <pre>
 * client to backend:  {"type": "emote", "emote": "wave"}     {"type": "emote_stop"}
 * backend to client:  {"type": "emote", "uuid": "...", "emote": "wave"}     {"type": "emote_stop", "uuid": "..."}
 * </pre>
 *
 * Incoming emotes must name a known emote and a player in your world, and are rate limited.
 */
public final class EmoteSync {
    private static final long MIN_SEND_GAP_MS = 400;
    private static final long MIN_RECEIVE_GAP_MS = 250;

    private final EmoteManager manager;
    private BackendSocket socket;
    private long lastSend;
    private final Map<UUID, Long> lastReceive = new HashMap<>();

    EmoteSync(EmoteManager manager) {
        this.manager = manager;
    }

    public void attach(BackendSocket socket) {
        this.socket = socket;
        socket.on("emote", this::onEmote);
        socket.on("emote_stop", this::onStop);
    }

    void sendStart(Emote emote) {
        if (socket == null) return;
        long now = Util.getMillis();
        if (now - lastSend < MIN_SEND_GAP_MS) return;
        lastSend = now;
        JsonObject m = new JsonObject();
        m.addProperty("type", "emote");
        m.addProperty("emote", emote.id());
        socket.send(m);
    }

    void sendStop() {
        if (socket == null) return;
        JsonObject m = new JsonObject();
        m.addProperty("type", "emote_stop");
        socket.send(m);
    }

    private void onEmote(JsonObject m) {
        UUID uuid = uuid(m);
        if (uuid == null || !m.has("emote")) return;
        Emote emote = manager.registry().get(m.get("emote").getAsString());
        if (emote == null) return;
        long now = Util.getMillis();
        Long last = lastReceive.get(uuid);
        if (last != null && now - last < MIN_RECEIVE_GAP_MS) return;
        if (lastReceive.size() > 256) lastReceive.clear();
        lastReceive.put(uuid, now);
        manager.playRemote(uuid, emote);
    }

    private void onStop(JsonObject m) {
        UUID uuid = uuid(m);
        if (uuid != null) manager.stopRemote(uuid);
    }

    private static UUID uuid(JsonObject m) {
        try {
            UUID uuid = UUID.fromString(m.get("uuid").getAsString());
            var player = Minecraft.getInstance().player;
            return player != null && uuid.equals(player.getUUID()) ? null : uuid;
        } catch (RuntimeException e) {
            return null;
        }
    }
}

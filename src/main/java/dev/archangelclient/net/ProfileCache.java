package dev.archangelclient.net;

import dev.archangelclient.ArchangelClient;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.util.Util;

/**
 * Other players' {@link UserProfile}s by UUID, used for cosmetics and nametags. Lookups never
 * block: a miss returns the cached value (or {@link UserProfile#NONE}) and queues a background
 * fetch. When the backend can't be reached the cache goes into offline mode for a while instead
 * of retrying every frame; offline, only you are an Archangel player.
 */
public final class ProfileCache {
    private static final long TTL_MS = 5 * 60_000;
    private static final long OFFLINE_BACKOFF_MS = 60_000;
    private static final int MAX_ENTRIES = 512;
    /** Offline-mode servers give players version 3 UUIDs; only honoured for local testing. */
    private static final boolean ALLOW_OFFLINE_UUIDS = Boolean.getBoolean("archangel.allowOfflineUuids");

    private final UserApi api;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Archangel profile fetch");
        t.setDaemon(true);
        return t;
    });
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private volatile long offlineUntil;
    private volatile boolean everReached;

    private record Entry(UserProfile profile, long fetchedAt, boolean pending) {}

    public ProfileCache(UserApi api) {
        this.api = api;
    }

    public UserProfile get(UUID uuid) {
        // real accounts have version 4 UUIDs; version 2 are server-side NPCs
        if (uuid.version() != 4 && !(ALLOW_OFFLINE_UUIDS && uuid.version() == 3)) return UserProfile.NONE;
        long now = Util.getMillis();
        Entry entry = entries.get(uuid);
        if (entry != null && (entry.pending() || now - entry.fetchedAt() < TTL_MS)) return entry.profile();
        if (now >= offlineUntil) fetch(uuid, entry, now);
        return entry != null ? entry.profile() : UserProfile.NONE;
    }

    public boolean isOffline() {
        return Util.getMillis() < offlineUntil || !everReached;
    }

    /** Drops a cached profile, e.g. when the backend says that player changed something. */
    public void invalidate(UUID uuid) {
        entries.remove(uuid);
    }

    public void clear() {
        entries.clear();
    }

    private void fetch(UUID uuid, Entry previous, long now) {
        if (entries.size() >= MAX_ENTRIES) entries.clear();
        UserProfile stale = previous != null ? previous.profile() : UserProfile.NONE;
        entries.put(uuid, new Entry(stale, now, true));
        executor.execute(() -> {
            try {
                UserProfile profile = api.fetch(uuid);
                everReached = true;
                entries.put(uuid, new Entry(profile, Util.getMillis(), false));
            } catch (Exception e) {
                if (Util.getMillis() >= offlineUntil) {
                    ArchangelClient.LOGGER.debug("Backend unreachable ({}); showing local cosmetics only", e.toString());
                }
                offlineUntil = Util.getMillis() + OFFLINE_BACKOFF_MS;
                entries.put(uuid, new Entry(stale, Util.getMillis(), false));
            }
        });
    }
}

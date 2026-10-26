package dev.archangelclient.cosmetics.net;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.cosmetics.Loadout;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.util.Util;

/**
 * Other players' loadouts by UUID. Lookups never block: a miss returns the cached value (or
 * nothing) and queues a background fetch. When the backend can't be reached the cache goes into
 * offline mode for a while instead of retrying every frame, and everyone renders without
 * cosmetics except the local player, whose loadout comes from the local profile.
 */
public final class CosmeticProfileCache {
    private static final long TTL_MS = 5 * 60_000;
    private static final long OFFLINE_BACKOFF_MS = 60_000;
    private static final int MAX_ENTRIES = 512;

    private final CosmeticsApi api;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Archangel cosmetics fetch");
        t.setDaemon(true);
        return t;
    });
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private volatile long offlineUntil;
    private volatile boolean everReached;

    private record Entry(Loadout loadout, long fetchedAt, boolean pending) {}

    public CosmeticProfileCache(CosmeticsApi api) {
        this.api = api;
    }

    public Loadout get(UUID uuid) {
        // only real accounts (v4 UUIDs) have cosmetics; v2 UUIDs are server-side NPCs
        if (uuid.version() != 4) return Loadout.EMPTY;
        long now = Util.getMillis();
        Entry entry = entries.get(uuid);
        if (entry != null && (entry.pending() || now - entry.fetchedAt() < TTL_MS)) return entry.loadout();
        if (now >= offlineUntil) fetch(uuid, entry, now);
        return entry != null ? entry.loadout() : Loadout.EMPTY;
    }

    public boolean isOffline() {
        return Util.getMillis() < offlineUntil || !everReached;
    }

    public void invalidate(UUID uuid) {
        entries.remove(uuid);
    }

    public void clear() {
        entries.clear();
    }

    private void fetch(UUID uuid, Entry previous, long now) {
        if (entries.size() >= MAX_ENTRIES) entries.clear();
        Loadout stale = previous != null ? previous.loadout() : Loadout.EMPTY;
        entries.put(uuid, new Entry(stale, now, true));
        executor.execute(() -> {
            try {
                Loadout loadout = api.fetchLoadout(uuid);
                everReached = true;
                entries.put(uuid, new Entry(loadout, Util.getMillis(), false));
            } catch (Exception e) {
                if (Util.getMillis() >= offlineUntil) {
                    ArchangelClient.LOGGER.debug("Cosmetics backend unreachable ({}); showing local cosmetics only", e.toString());
                }
                offlineUntil = Util.getMillis() + OFFLINE_BACKOFF_MS;
                entries.put(uuid, new Entry(stale, Util.getMillis(), false));
            }
        });
    }
}

package dev.archangelclient.net;

import com.mojang.authlib.exceptions.AuthenticationException;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.cosmetics.Loadout;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * Signs in to the Archangel backend and keeps the session token. Sign-in uses the same proof
 * Minecraft servers use: we call Mojang's joinServer with a random serverId (your access token only
 * ever goes to Mojang, never to Archangel), then the backend asks Mojang whether you joined it.
 *
 * <p>Sign-in only runs while you're in a world, so it can never interfere with the handshake of a
 * server you're connecting to. Without a backend, or with an offline account, everything stays
 * local and nothing is retried more than every few minutes.
 */
public final class BackendSession {
    /** Renew this long before the token expires. */
    private static final Duration RENEW_BEFORE = Duration.ofMinutes(10);
    /** After a failed sign-in, wait this long before trying again. */
    private static final Duration RETRY_AFTER = Duration.ofMinutes(3);
    /** Wait for more changes before uploading the loadout, so dragging through the Locker is one upload. */
    private static final long LOADOUT_DEBOUNCE_MS = 1500;
    private static final int SERVER_ID_BYTES = 20;

    private final UserApi api;
    private final Supplier<Loadout> loadout;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(daemon("Archangel sign-in"));
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(daemon("Archangel loadout sync"));
    private final SecureRandom random = new SecureRandom();

    private volatile UserApi.@Nullable Session session;
    private volatile @Nullable UUID sessionOwner;
    private volatile Instant nextAttempt = Instant.EPOCH;
    private @Nullable ScheduledFuture<?> pendingUpload;

    public BackendSession(UserApi api, Supplier<Loadout> loadout) {
        this.api = api;
        this.loadout = loadout;
    }

    /**
     * The current token, signing in first if needed (blocking; never call on the render thread).
     * Null when offline or not signed in.
     */
    public @Nullable String tokenBlocking() {
        try {
            return CompletableFuture.supplyAsync(this::ensureSignedIn, worker).get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    /** Schedules an upload of the local loadout (after a short pause for further changes). */
    public synchronized void loadoutChanged() {
        if (pendingUpload != null) pendingUpload.cancel(false);
        pendingUpload = timer.schedule(() -> worker.execute(this::upload), LOADOUT_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    /** Forgets the token, e.g. when the account changes. */
    public void reset() {
        session = null;
        sessionOwner = null;
        nextAttempt = Instant.EPOCH;
    }

    private void upload() {
        String token = currentToken();
        if (token == null) return; // offline or never signed in: the loadout is pushed after the next sign-in
        try {
            api.putCosmetics(token, loadout.get());
        } catch (Exception e) {
            ArchangelClient.LOGGER.debug("Couldn't save cosmetics to the backend: {}", e.toString());
        }
    }

    private @Nullable String currentToken() {
        UserApi.Session s = session;
        Minecraft mc = Minecraft.getInstance();
        if (s == null || !mc.getUser().getProfileId().equals(sessionOwner)) return null;
        return Instant.now().isBefore(s.expiresAt()) ? s.token() : null;
    }

    /** Runs on the worker thread. */
    private @Nullable String ensureSignedIn() {
        String token = currentToken();
        UserApi.Session s = session;
        if (token != null && s != null && Instant.now().isBefore(s.expiresAt().minus(RENEW_BEFORE))) return token;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || Instant.now().isBefore(nextAttempt)) return token;
        nextAttempt = Instant.now().plus(RETRY_AFTER);

        UUID profileId = mc.getUser().getProfileId();
        String name = mc.getUser().getName();
        UserApi.Session fresh = null;
        try {
            String serverId = HexFormat.of().formatHex(randomBytes());
            mc.services().sessionService().joinServer(profileId, mc.getUser().getAccessToken(), serverId);
            fresh = api.signIn(name, serverId);
        } catch (AuthenticationException e) {
            // offline or development account: Mojang won't vouch for it
            if (Boolean.getBoolean("archangel.devAuth")) fresh = devSignIn(profileId, name);
        } catch (Exception e) {
            ArchangelClient.LOGGER.debug("Archangel sign-in failed: {}", e.toString());
        }
        if (fresh == null) return token;
        session = fresh;
        sessionOwner = profileId;
        nextAttempt = Instant.EPOCH;
        ArchangelClient.LOGGER.info("Signed in to the Archangel backend as {}", name);
        upload(); // the backend now shows what the active profile has equipped
        return fresh.token();
    }

    private UserApi.@Nullable Session devSignIn(UUID profileId, String name) {
        try {
            return api.devSignIn(profileId, name);
        } catch (Exception e) {
            ArchangelClient.LOGGER.debug("Archangel dev sign-in failed: {}", e.toString());
            return null;
        }
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[SERVER_ID_BYTES];
        random.nextBytes(bytes);
        return bytes;
    }

    private static java.util.concurrent.ThreadFactory daemon(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }
}

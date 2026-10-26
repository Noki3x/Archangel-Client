package dev.archangelclient.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.archangelclient.ArchangelClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * The live connection to the Archangel backend ({@code ws://<api>/v1/ws}), open while you are in a
 * world. Messages are JSON objects with a {@code "type"}; handlers run on the client thread.
 * Without a backend it retries quietly with a growing delay, and every feature works offline.
 *
 * <p>Only Archangel's own data goes over this socket (emotes, later cosmetics updates); it never
 * talks to the game server.
 */
public final class BackendSocket {
    public static final String DEFAULT_URL = "http://127.0.0.1:8787";
    private static final long MIN_RETRY_MS = 5_000, MAX_RETRY_MS = 60_000;

    private final URI uri;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Archangel backend socket");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, Consumer<JsonObject>> handlers = new ConcurrentHashMap<>();
    private volatile @Nullable WebSocket socket;
    private volatile @Nullable Supplier<JsonObject> hello;
    private volatile long retryMs = MIN_RETRY_MS;
    private volatile int generation;

    public BackendSocket() {
        String base = System.getProperty("archangel.api", DEFAULT_URL);
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        uri = URI.create(base.replaceFirst("^http", "ws") + "/v1/ws");
    }

    public void on(String type, Consumer<JsonObject> handler) {
        handlers.put(type, handler);
    }

    public boolean isConnected() {
        return socket != null;
    }

    /** Connects (and keeps reconnecting) until {@link #disconnect()}; {@code hello} is sent on every connect. */
    public void connect(Supplier<JsonObject> hello) {
        this.hello = hello;
        int gen = ++generation;
        retryMs = MIN_RETRY_MS;
        scheduler.execute(() -> open(gen));
    }

    public void disconnect() {
        generation++;
        hello = null;
        WebSocket ws = socket;
        socket = null;
        if (ws != null) ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye").exceptionally(e -> null);
    }

    /** Sends if connected; messages while offline are dropped (they are all transient). */
    public void send(JsonObject message) {
        WebSocket ws = socket;
        if (ws == null) return;
        String text = message.toString();
        scheduler.execute(() -> {
            try {
                ws.sendText(text, true).get(4, TimeUnit.SECONDS);
            } catch (Exception e) {
                ArchangelClient.LOGGER.debug("Backend send failed: {}", e.toString());
            }
        });
    }

    private void open(int gen) {
        if (gen != generation || hello == null) return;
        http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(4)).buildAsync(uri, new Listener(gen))
                .whenComplete((ws, error) -> {
                    if (error != null) {
                        retry(gen);
                        return;
                    }
                    if (gen != generation) {
                        ws.sendClose(WebSocket.NORMAL_CLOSURE, "stale");
                        return;
                    }
                    socket = ws;
                    retryMs = MIN_RETRY_MS;
                    ArchangelClient.LOGGER.info("Connected to the Archangel backend at {}", uri);
                    Supplier<JsonObject> h = hello;
                    if (h != null) send(h.get());
                });
    }

    private void retry(int gen) {
        if (gen != generation) return;
        socket = null;
        long delay = retryMs;
        retryMs = Math.min(MAX_RETRY_MS, retryMs * 2);
        scheduler.schedule(() -> open(gen), delay, TimeUnit.MILLISECONDS);
    }

    private final class Listener implements WebSocket.Listener {
        private final int gen;
        private final StringBuilder partial = new StringBuilder();
        private boolean ended;

        Listener(int gen) {
            this.gen = gen;
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                String text = partial.toString();
                partial.setLength(0);
                dispatch(text);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
            if (socket == ws) ArchangelClient.LOGGER.info("Backend closed the connection ({} {})", status, reason);
            end();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            ArchangelClient.LOGGER.debug("Backend socket error: {}", error.toString());
            end();
        }

        /** onClose and onError can both fire for one connection; reconnect once. */
        private synchronized void end() {
            if (ended) return;
            ended = true;
            retry(gen);
        }

        private void dispatch(String text) {
            if (text.length() > 16_384) return;
            JsonObject message;
            try {
                message = JsonParser.parseString(text).getAsJsonObject();
            } catch (RuntimeException e) {
                return;
            }
            Consumer<JsonObject> handler = message.has("type") ? handlers.get(message.get("type").getAsString()) : null;
            if (handler != null) Minecraft.getInstance().execute(() -> handler.accept(message));
        }
    }
}

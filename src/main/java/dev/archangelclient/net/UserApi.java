package dev.archangelclient.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.archangelclient.cosmetics.Loadout;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * REST client for the Archangel backend (see backend/README.md): public profiles
 * ({@code GET /users/:uuid}, read by {@link UserProfile#fromJson}), sign-in ({@code POST /auth})
 * and saving your loadout ({@code PUT /me/cosmetics}). The base URL defaults to a local
 * development server and can be overridden with the {@code archangel.api} system property.
 * All calls block; use them off the render thread.
 */
public final class UserApi {
    public static final String DEFAULT_URL = "http://127.0.0.1:8787";
    private static final Duration TIMEOUT = Duration.ofSeconds(4);
    private static final int MAX_BODY = 16_384;

    private final String baseUrl;
    private final HttpClient http;

    public UserApi() {
        this(baseUrl());
    }

    public UserApi(String baseUrl) {
        this.baseUrl = baseUrl;
        // plain HTTP/1.1: the default HTTP/2 upgrade attempt is refused by servers that also host a WebSocket
        this.http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(TIMEOUT).build();
    }

    /** The backend's base URL, without a trailing slash. */
    public static String baseUrl() {
        String url = System.getProperty("archangel.api", DEFAULT_URL);
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Blocking; call off the render thread. Returns {@link UserProfile#NONE} for players who don't use Archangel. */
    public UserProfile fetch(UUID uuid) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/users/" + uuid))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) return UserProfile.NONE;
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
        if (response.body().length() > MAX_BODY) throw new IOException("response too large");
        return UserProfile.fromJson(JsonParser.parseString(response.body()).getAsJsonObject());
    }

    /** A session token from the backend and when it stops working. */
    public record Session(String token, Instant expiresAt) {}

    /**
     * Exchanges a Mojang join for a session token. The caller must already have called Mojang's
     * joinServer with {@code serverId}; the backend checks it with Mojang's hasJoined.
     * Returns null if the backend didn't accept it.
     */
    public Session signIn(String username, String serverId) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("serverId", serverId);
        return session(post("/auth", body));
    }

    /** Development sign-in for offline accounts; only a backend with ALLOW_UNVERIFIED answers it. */
    public Session devSignIn(UUID uuid, String username) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", uuid.toString());
        body.addProperty("username", username);
        return session(post("/auth/dev", body));
    }

    /** Replaces your equipped cosmetics on the backend. */
    public void putCosmetics(String token, Loadout loadout) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        loadout.asMap().forEach((type, id) -> body.addProperty(type.key(), id));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/me/cosmetics"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status != 200) throw new IOException("HTTP " + status);
    }

    private HttpResponse<String> post(String path, JsonObject body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Session session(HttpResponse<String> response) throws IOException {
        if (response.statusCode() == 401 || response.statusCode() == 404) return null;
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
        if (response.body().length() > MAX_BODY) throw new IOException("response too large");
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return new Session(json.get("token").getAsString(), Instant.parse(json.get("expiresAt").getAsString()));
    }
}

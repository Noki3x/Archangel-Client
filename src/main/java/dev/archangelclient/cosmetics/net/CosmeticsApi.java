package dev.archangelclient.cosmetics.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.archangelclient.cosmetics.Loadout;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Client for the Archangel backend's cosmetics endpoint (the backend itself arrives in milestone
 * 9). Only cosmetic loadouts are exchanged: {@code GET /v1/cosmetics/<uuid>} returns
 * {@code {"uuid": "...", "equipped": {"cape": "cape_nebula", ...}}}.
 *
 * <p>The base URL defaults to a local development server and can be overridden with the
 * {@code archangel.api} system property.
 */
public final class CosmeticsApi {
    public static final String DEFAULT_URL = "http://127.0.0.1:8787";
    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final String baseUrl;
    private final HttpClient http;

    public CosmeticsApi() {
        this(System.getProperty("archangel.api", DEFAULT_URL));
    }

    public CosmeticsApi(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    /** Blocking; call off the render thread. Returns {@link Loadout#EMPTY} for players without cosmetics. */
    public Loadout fetchLoadout(UUID uuid) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/cosmetics/" + uuid))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) return Loadout.EMPTY;
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        return body.has("equipped") ? Loadout.fromJson(body.getAsJsonObject("equipped")) : Loadout.EMPTY;
    }
}

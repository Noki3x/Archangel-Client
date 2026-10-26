package dev.archangelclient.net;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * REST client for the Archangel backend's public profile endpoint, {@code GET /users/:uuid}
 * (see {@link UserProfile#fromJson} for the response). The base URL defaults to a local
 * development server and can be overridden with the {@code archangel.api} system property.
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
}

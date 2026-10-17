package com.minewaku.chatter.identityaccess.start.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/** Minimal HTTP client for the running service: one method per kind of call, JSON in and out. */
public final class ApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String baseUrl;

    public ApiClient(int port) {
        this.baseUrl = "http://localhost:" + port + "/api/v1";
    }

    public ApiResponse post(String path, Object body, String accessToken, String userAgent) {
        return send(builder(path, accessToken, userAgent)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))));
    }

    public ApiResponse post(String path, Object body, String accessToken) {
        return post(path, body, accessToken, null);
    }

    public ApiResponse post(String path, Object body) {
        return post(path, body, null, null);
    }

    public ApiResponse put(String path, Object body, String accessToken) {
        return send(builder(path, accessToken, null)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))));
    }

    public ApiResponse postRaw(String path, String rawBody, String contentType) {
        return send(builder(path, null, null)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(rawBody)));
    }

    public ApiResponse get(String path, String accessToken) {
        return send(builder(path, accessToken, null).GET());
    }

    public ApiResponse delete(String path, String accessToken) {
        return send(builder(path, accessToken, null).DELETE());
    }

    /** Absolute path against the server root (for example {@code /v3/api-docs}), without the API prefix. */
    public ApiResponse getRoot(String path) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl.replace("/api/v1", "") + path))
                .GET();
        return send(request);
    }

    public static Map<String, Object> credentials(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    private HttpRequest.Builder builder(String path, String accessToken, String userAgent) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        if (userAgent != null) {
            request.header("User-Agent", userAgent);
        }
        return request;
    }

    private ApiResponse send(HttpRequest.Builder request) {
        try {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(response.statusCode(), response.headers(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

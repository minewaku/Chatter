package com.minewaku.chatter.identityaccess.start.support;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

/**
 * One Vault in dev mode (in-memory, KV version 2 already mounted at {@code secret/}) shared by every test of the JVM.
 * Secrets are seeded and removed over Vault's HTTP API.
 */
public final class VaultContainerSupport {

    public static final String ROOT_TOKEN = "test-root-token";

    private static final GenericContainer<?> VAULT = new GenericContainer<>(
                    DockerImageName.parse("hashicorp/vault:1.17.6"))
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", ROOT_TOKEN)
            .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
            .withEnv("SKIP_SETCAP", "true")
            .withExposedPorts(8200)
            .waitingFor(Wait.forHttp("/v1/sys/health").forPort(8200));

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    static {
        VAULT.start();
    }

    private VaultContainerSupport() {}

    public static String host() {
        return VAULT.getHost();
    }

    public static int port() {
        return VAULT.getMappedPort(8200);
    }

    /** A client for the container using {@code token}; a wrong token gives a rejected credential. */
    public static VaultTemplate template(String token) {
        return template(host(), port(), token);
    }

    public static VaultTemplate template(String host, int port, String token) {
        VaultEndpoint endpoint = VaultEndpoint.create(host, port);
        endpoint.setScheme("http");
        return new VaultTemplate(endpoint, new TokenAuthentication(token));
    }

    /** A local port nothing listens on. */
    public static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void put(String path, Map<String, String> fields) {
        send("POST", "/v1/secret/data/" + path, JSON.writeValueAsString(Map.of("data", fields)));
    }

    public static void delete(String path) {
        send("DELETE", "/v1/secret/metadata/" + path, null);
    }

    private static void send(String method, String uriPath, String body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://" + host() + ":" + port() + uriPath))
                .header("X-Vault-Token", ROOT_TOKEN)
                .header("Content-Type", "application/json")
                .method(
                        method,
                        body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        try {
            HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Vault " + method + " " + uriPath + " answered " + response.statusCode());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

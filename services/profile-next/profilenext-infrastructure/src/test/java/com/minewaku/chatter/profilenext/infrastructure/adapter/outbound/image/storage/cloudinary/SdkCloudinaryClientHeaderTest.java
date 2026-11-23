package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The header read against a local server standing in for the delivery address; no Cloudinary account is touched. */
class SdkCloudinaryClientHeaderTest {

    private static final byte[] FILE = new byte[10_000];

    static {
        for (int i = 0; i < FILE.length; i++) {
            FILE[i] = (byte) i;
        }
    }

    private HttpServer server;
    private final Map<String, String> seenRanges = new ConcurrentHashMap<>();
    private SdkCloudinaryClient client;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // honours Range
        server.createContext("/ranged/", exchange -> {
            String range = exchange.getRequestHeaders().getFirst("Range");
            seenRanges.put(exchange.getRequestURI().getPath(), range);
            int end = Integer.parseInt(range.substring("bytes=0-".length()));
            byte[] body = Arrays.copyOfRange(FILE, 0, Math.min(end + 1, FILE.length));
            exchange.sendResponseHeaders(206, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        // ignores Range and sends everything
        server.createContext("/whole/", exchange -> {
            exchange.sendResponseHeaders(200, FILE.length);
            exchange.getResponseBody().write(FILE);
            exchange.close();
        });
        server.createContext("/gone/", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new SdkCloudinaryClient(
                new Cloudinary(ObjectUtils.asMap("cloud_name", "demo", "api_key", "k", "api_secret", "s")),
                base,
                Duration.ofSeconds(5));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void asksForTheFirstBytesOfTheOriginalAndGetsThem() {
        byte[] head = client.readLeadingBytes("ranged/profiles/42/avatar/x", 512);

        assertThat(head).isEqualTo(Arrays.copyOfRange(FILE, 0, 512));
        assertThat(seenRanges).containsValue("bytes=0-511");
    }

    @Test
    void aShorterFileIsReturnedWhole() {
        assertThat(client.readLeadingBytes("ranged/profiles/42/avatar/x", 50_000)).isEqualTo(FILE);
    }

    @Test
    void aServerThatIgnoresRangeStillYieldsOnlyTheBytesAsked() {
        assertThat(client.readLeadingBytes("whole/profiles/42/avatar/x", 256)).isEqualTo(Arrays.copyOfRange(FILE, 0, 256));
    }

    @Test
    void aMissingFileIsUnavailable() {
        assertThatThrownBy(() -> client.readLeadingBytes("gone/profiles/42/avatar/x", 256))
                .isInstanceOf(ImageStorageUnavailableException.class);
    }

    @Test
    void anUnreachableServerIsUnavailable() {
        server.stop(0);

        assertThatThrownBy(() -> client.readLeadingBytes("ranged/profiles/42/avatar/x", 256))
                .isInstanceOf(ImageStorageUnavailableException.class);
    }
}

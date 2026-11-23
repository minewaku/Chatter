package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.r2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.r2.ObjectStoreClient.PresignedPut;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.UploadTicket;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R2ImageStorageTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final OwnedImage IMAGE = new OwnedImage(
            UserId.of(42), ImageSlot.AVATAR, new ImageId(UUID.fromString("0197aaaa-0000-7000-8000-000000000001")));
    private static final String KEY = "profiles/42/avatar/0197aaaa-0000-7000-8000-000000000001";

    /** Records every call and answers from a map; fails on demand. */
    static class FakeClient implements ObjectStoreClient {
        final List<String> calls = new ArrayList<>();
        final Map<String, byte[]> objects = new LinkedHashMap<>();
        RuntimeException failure;
        Map<String, String> signedHeaders = Map.of(
                "host", "storage.test", "content-type", "image/png", "content-length", "1000", "if-none-match", "*");

        @Override
        public PresignedPut presignPut(String bucket, String key, String contentType, long contentLength, Duration validFor) {
            calls.add("presign:" + bucket + "/" + key + ":" + validFor);
            fail();
            return new PresignedPut("https://storage.test/" + bucket + "/" + key + "?sig", signedHeaders);
        }

        @Override
        public Optional<byte[]> readRange(String bucket, String key, long from, long toInclusive) {
            calls.add("range:" + bucket + "/" + key + ":" + from + "-" + toInclusive);
            fail();
            return Optional.ofNullable(objects.get(key));
        }

        @Override
        public void delete(String bucket, String key) {
            calls.add("delete:" + bucket + "/" + key);
            fail();
        }

        private void fail() {
            if (failure != null) {
                throw failure;
            }
        }
    }

    private final FakeClient client = new FakeClient();

    private R2ImageStorage storage(String accessKey, String bucket) {
        R2Properties properties = new R2Properties("acct", null, null, bucket, accessKey, "secret", null, null);
        return new R2ImageStorage(client, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private R2ImageStorage configured() {
        return storage("access", "media");
    }

    @Test
    void theTicketTargetsTheKeyOfTheImageAndCarriesEverySignedHeaderExceptHost() {
        UploadTicket ticket = configured().issueUpload(IMAGE, "image/png", 1000, Duration.ofMinutes(5));

        assertThat(client.calls).containsExactly("presign:media/" + KEY + ":PT5M");
        assertThat(ticket.method()).isEqualTo("PUT");
        assertThat(ticket.url()).startsWith("https://storage.test/media/" + KEY);
        assertThat(ticket.headers())
                .containsEntry("Content-Type", "image/png")
                .containsEntry("Content-Length", "1000")
                .containsEntry("if-none-match", "*")
                .doesNotContainKey("host");
        assertThat(ticket.fields()).isEmpty();
        assertThat(ticket.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void theFirstBytesAreOneRangeFromTheStartOfTheObject() {
        client.objects.put(KEY, new byte[] {1, 2, 3});

        Optional<byte[]> bytes = configured().readFirstBytes(IMAGE, 100);

        assertThat(bytes).hasValueSatisfying(value -> assertThat(value).containsExactly(1, 2, 3));
        assertThat(client.calls).containsExactly("range:media/" + KEY + ":0-99");
    }

    @Test
    void anObjectThatHasNotArrivedIsEmpty() {
        assertThat(configured().readFirstBytes(IMAGE, 100)).isEmpty();
    }

    @Test
    void deleteTargetsTheSameKey() {
        configured().delete(IMAGE);

        assertThat(client.calls).containsExactly("delete:media/" + KEY);
    }

    @Test
    void withoutCredentialsEveryCallIsUnavailableAndNothingIsSent() {
        R2ImageStorage unconfigured = storage("", "media");

        assertThatThrownBy(() -> unconfigured.issueUpload(IMAGE, "image/png", 1, Duration.ofMinutes(1)))
                .isInstanceOf(ImageStorageUnavailableException.class);
        assertThatThrownBy(() -> unconfigured.readFirstBytes(IMAGE, 10)).isInstanceOf(ImageStorageUnavailableException.class);
        assertThatThrownBy(() -> unconfigured.delete(IMAGE)).isInstanceOf(ImageStorageUnavailableException.class);
        assertThat(client.calls).isEmpty();
    }

    @Test
    void anythingTheClientThrowsSurfacesAsUnavailable() {
        client.failure = new IllegalStateException("sdk blew up");

        assertThatThrownBy(() -> configured().readFirstBytes(IMAGE, 10))
                .isInstanceOf(ImageStorageUnavailableException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}

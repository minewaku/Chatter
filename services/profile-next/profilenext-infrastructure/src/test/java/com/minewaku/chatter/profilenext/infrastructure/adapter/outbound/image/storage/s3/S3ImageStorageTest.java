package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3.ObjectStoreClient.ObjectHead;
import com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3.ObjectStoreClient.PresignedPut;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.StoredObject;
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

class S3ImageStorageTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final OwnedImage IMAGE = new OwnedImage(
            UserId.of(42), ImageSlot.AVATAR, new ImageId(UUID.fromString("0197aaaa-0000-7000-8000-000000000001")));
    private static final String KEY = "profiles/42/avatar/0197aaaa-0000-7000-8000-000000000001";

    /** Records every call and answers from a map; fails on demand. */
    static class FakeClient implements ObjectStoreClient {
        final List<String> calls = new ArrayList<>();
        final Map<String, ObjectHead> heads = new LinkedHashMap<>();
        RuntimeException failure;
        Map<String, String> signedHeaders = Map.of("host", "storage.test", "content-type", "image/png", "content-length", "1000", "if-none-match", "*");

        @Override
        public PresignedPut presignPut(String bucket, String key, String contentType, long contentLength, Duration validFor) {
            calls.add("presign:" + bucket + "/" + key + ":" + validFor);
            fail();
            return new PresignedPut("https://storage.test/" + bucket + "/" + key + "?sig", signedHeaders);
        }

        @Override
        public Optional<ObjectHead> head(String bucket, String key) {
            calls.add("head:" + bucket + "/" + key);
            fail();
            return Optional.ofNullable(heads.get(key));
        }

        @Override
        public byte[] readRange(String bucket, String key, long from, long toInclusive) {
            calls.add("range:" + bucket + "/" + key + ":" + from + "-" + toInclusive);
            fail();
            return new byte[] {1, 2, 3};
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

    private S3ImageStorage storage(String accessKey, String bucket) {
        ImageStorageProperties properties = new ImageStorageProperties(
                "", "auto", true, accessKey, "secret", bucket, "https://cdn.test", null, null, null, null, null, null,
                null, null, null);
        return new S3ImageStorage(client, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private S3ImageStorage configured() {
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
    void statReportsTheLengthAndTheRecordedContentType() {
        client.heads.put(KEY, new ObjectHead(1234, "image/webp"));

        assertThat(configured().stat(IMAGE)).contains(new StoredObject(1234, "image/webp"));
    }

    @Test
    void statOfAMissingObjectIsEmpty() {
        assertThat(configured().stat(IMAGE)).isEmpty();
    }

    @Test
    void theHeaderIsARangeFromTheStartOfTheObject() {
        byte[] header = configured().readHeader(IMAGE, 100);

        assertThat(header).containsExactly(1, 2, 3);
        assertThat(client.calls).containsExactly("range:media/" + KEY + ":0-99");
    }

    @Test
    void deleteTargetsTheSameKey() {
        configured().delete(IMAGE);

        assertThat(client.calls).containsExactly("delete:media/" + KEY);
    }

    @Test
    void aRawObjectStoreDoesNotValidateWhatItReceives() {
        assertThat(configured().validatesOnUpload()).isFalse();
    }

    @Test
    void withoutCredentialsEveryCallIsUnavailableAndNothingIsSent() {
        S3ImageStorage unconfigured = storage("", "media");

        assertThatThrownBy(() -> unconfigured.issueUpload(IMAGE, "image/png", 1, Duration.ofMinutes(1)))
                .isInstanceOf(ImageStorageUnavailableException.class);
        assertThatThrownBy(() -> unconfigured.stat(IMAGE)).isInstanceOf(ImageStorageUnavailableException.class);
        assertThatThrownBy(() -> unconfigured.readHeader(IMAGE, 10)).isInstanceOf(ImageStorageUnavailableException.class);
        assertThatThrownBy(() -> unconfigured.delete(IMAGE)).isInstanceOf(ImageStorageUnavailableException.class);
        assertThat(client.calls).isEmpty();
    }

    @Test
    void anythingTheClientThrowsSurfacesAsUnavailable() {
        client.failure = new IllegalStateException("sdk blew up");

        assertThatThrownBy(() -> configured().stat(IMAGE))
                .isInstanceOf(ImageStorageUnavailableException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}

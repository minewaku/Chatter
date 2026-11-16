package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary.CloudinaryClient.Asset;
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

class CloudinaryImageStorageTest {

    private static final String SECRET = "test-api-secret";
    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final OwnedImage IMAGE = new OwnedImage(
            UserId.of(42), ImageSlot.AVATAR, new ImageId(UUID.fromString("0197aaaa-0000-7000-8000-000000000001")));
    private static final String PUBLIC_ID = "profiles/42/avatar/0197aaaa-0000-7000-8000-000000000001";

    /** Signs with the SDK itself, exactly as the production client does, and answers the rest from a map. */
    static class FakeClient implements CloudinaryClient {
        final Cloudinary sdk =
                new Cloudinary(ObjectUtils.asMap("cloud_name", "demo", "api_key", "key-1", "api_secret", SECRET));
        final List<String> calls = new ArrayList<>();
        final Map<String, Asset> assets = new LinkedHashMap<>();
        RuntimeException failure;

        @Override
        public String sign(Map<String, Object> params) {
            calls.add("sign");
            fail();
            return sdk.apiSignRequest(params, SECRET, sdk.config.signatureVersion);
        }

        @Override
        public Optional<Asset> asset(String publicId) {
            calls.add("asset:" + publicId);
            fail();
            return Optional.ofNullable(assets.get(publicId));
        }

        @Override
        public byte[] readHeader(String publicId, int maxBytes) {
            calls.add("header:" + publicId + ":" + maxBytes);
            fail();
            return new byte[] {1, 2, 3};
        }

        @Override
        public void destroy(String publicId) {
            calls.add("destroy:" + publicId);
            fail();
        }

        private void fail() {
            if (failure != null) {
                throw failure;
            }
        }
    }

    private final FakeClient client = new FakeClient();

    private static ImageStorageProperties properties(String apiSecret) {
        return new ImageStorageProperties(
                "demo", "key-1", apiSecret, null, null, null, null, null, null, null, null, null, null);
    }

    private CloudinaryImageStorage storage() {
        return new CloudinaryImageStorage(client, properties(SECRET), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void theTicketIsASignedFormPostToTheImagePathWithoutOverwrite() {
        UploadTicket ticket = storage().issueUpload(IMAGE, "image/png", 1_000, Duration.ofMinutes(5));

        assertThat(ticket.url()).isEqualTo("https://api.cloudinary.com/v1_1/demo/image/upload");
        assertThat(ticket.method()).isEqualTo("POST");
        assertThat(ticket.headers()).isEmpty();
        assertThat(ticket.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(ticket.fields())
                .containsEntry("public_id", PUBLIC_ID)
                .containsEntry("overwrite", "false")
                .containsEntry("allowed_formats", "jpg,png,webp")
                .containsEntry("timestamp", Long.toString(NOW.getEpochSecond()))
                .containsEntry("api_key", "key-1")
                .containsKey("signature");
    }

    @Test
    void theSignatureCoversEveryFieldTheClientSendsExceptTheKeyAndTheSignature() {
        UploadTicket ticket = storage().issueUpload(IMAGE, "image/png", 1_000, Duration.ofMinutes(5));

        Map<String, Object> signed = new LinkedHashMap<>(ticket.fields());
        signed.remove("api_key");
        signed.remove("signature");
        assertThat(ticket.fields().get("signature"))
                .isEqualTo(client.sdk.apiSignRequest(signed, SECRET, client.sdk.config.signatureVersion));
    }

    @Test
    void aStoredAssetIsReportedWithItsSizeAndContentTypeFromTheFormat() {
        client.assets.put(PUBLIC_ID, new Asset(1234, "jpg"));

        Optional<StoredObject> stored = storage().stat(IMAGE);

        assertThat(stored).contains(new StoredObject(1234, "image/jpeg"));
    }

    @Test
    void otherFormatsKeepTheirOwnNameSoThePolicyCanRefuseThem() {
        client.assets.put(PUBLIC_ID, new Asset(10, "gif"));

        assertThat(storage().stat(IMAGE)).contains(new StoredObject(10, "image/gif"));
    }

    @Test
    void nothingStoredIsEmpty() {
        assertThat(storage().stat(IMAGE)).isEmpty();
    }

    @Test
    void theHeaderIsReadFromTheImagePathAndTheServiceSniffsItItself() {
        CloudinaryImageStorage storage = storage();

        assertThat(storage.readHeader(IMAGE, 512)).containsExactly(1, 2, 3);
        assertThat(client.calls).containsExactly("header:" + PUBLIC_ID + ":512");
        assertThat(storage.validatesOnUpload()).isFalse();
    }

    @Test
    void deleteTargetsTheImagePath() {
        storage().delete(IMAGE);

        assertThat(client.calls).containsExactly("destroy:" + PUBLIC_ID);
    }

    @Test
    void anyClientFailureSurfacesAsUnavailable() {
        client.failure = new IllegalStateException("boom");
        CloudinaryImageStorage storage = storage();

        assertThatThrownBy(() -> storage.issueUpload(IMAGE, "image/png", 1, Duration.ofMinutes(1)))
                .isInstanceOf(ImageStorageUnavailableException.class);
        assertThatThrownBy(() -> storage.stat(IMAGE)).isInstanceOf(ImageStorageUnavailableException.class);
        assertThatThrownBy(() -> storage.readHeader(IMAGE, 10)).isInstanceOf(ImageStorageUnavailableException.class);
        assertThatThrownBy(() -> storage.delete(IMAGE)).isInstanceOf(ImageStorageUnavailableException.class);
    }

    @Test
    void withoutCredentialsEveryCallIsUnavailableAndTheProviderIsNeverTouched() {
        CloudinaryImageStorage unconfigured =
                new CloudinaryImageStorage(client, properties(""), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> unconfigured.issueUpload(IMAGE, "image/png", 1, Duration.ofMinutes(1)))
                .isInstanceOf(ImageStorageUnavailableException.class);
        assertThatThrownBy(() -> unconfigured.stat(IMAGE)).isInstanceOf(ImageStorageUnavailableException.class);
        assertThat(client.calls).isEmpty();
    }

    @Test
    void thePropertyDumpNeverShowsTheCredentials() {
        assertThat(properties(SECRET).toString()).doesNotContain(SECRET).doesNotContain("key-1");
    }
}

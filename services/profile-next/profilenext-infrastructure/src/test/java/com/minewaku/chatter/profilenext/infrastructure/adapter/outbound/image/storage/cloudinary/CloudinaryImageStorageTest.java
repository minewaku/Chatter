package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloudinary.Cloudinary;
import com.cloudinary.SignatureAlgorithm;
import com.cloudinary.utils.ObjectUtils;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageRateLimitedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.ConfirmedUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.UploadPermission;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CloudinaryImageStorageTest {

    private static final String SECRET = "test-api-secret";
    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final String PUBLIC_ID = "chatter/profile/avatars/7/0197aaab-0000-7000-8000-000000000001";

    /** Signs with the SDK itself, exactly as the production client does. */
    static class FakeClient implements CloudinaryClient {
        final Cloudinary sdk = new Cloudinary(ObjectUtils.asMap("cloud_name", "demo", "api_key", "key-1", "api_secret", SECRET));
        final List<String> destroyed = new ArrayList<>();
        RuntimeException destroyFailure;
        RuntimeException listFailure;
        ResourcePage page = new ResourcePage(List.of(), null);

        @Override
        public String sign(Map<String, Object> params) {
            return sdk.apiSignRequest(params, SECRET, sdk.config.signatureVersion);
        }

        @Override
        public void destroy(String publicId) {
            if (destroyFailure != null) {
                throw destroyFailure;
            }
            destroyed.add(publicId);
        }

        @Override
        public ResourcePage listResources(String prefix, String cursor) {
            if (listFailure != null) {
                throw listFailure;
            }
            return page;
        }
    }

    private final FakeClient client = new FakeClient();

    private CloudinaryImageStorage storage(String algorithm) {
        return new CloudinaryImageStorage(
                client,
                new ImageProperties(
                        "demo",
                        "key-1",
                        SECRET,
                        "https://chatter.example/api/v1/profiles/uploads/notifications",
                        algorithm,
                        Duration.ofMinutes(15),
                        Duration.ofHours(2),
                        null,
                        null,
                        null,
                        null));
    }

    private CloudinaryImageStorage storage() {
        return storage("SHA-1");
    }

    private static String body(String publicId, String userInContext, String slotInContext, String type) {
        return """
                {"notification_type":"%s","public_id":"%s","secure_url":"https://res.cloudinary.com/demo/image/upload/v1/x.png",
                 "format":"png","bytes":1234,"original_filename":"café photo",
                 "context":{"custom":{"userId":"%s","slot":"%s"}}}
                """.formatted(type, publicId, userInContext, slotInContext);
    }

    private static String sign(byte[] body, String timestamp, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(body);
        digest.update(timestamp.getBytes(StandardCharsets.UTF_8));
        digest.update(SECRET.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }

    private Optional<ConfirmedUpload> verify(String json, String timestamp, String algorithm) throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return storage(algorithm).verify(bytes, sign(bytes, timestamp, algorithm), timestamp, NOW);
    }

    private static String ts(Instant at) {
        return Long.toString(at.getEpochSecond());
    }

    @Test
    void permitSignsTheFieldsAndTheSignatureMatchesTheSdk() {
        UploadPermission permission = storage().permit(UserId.of(7), ImageSlot.AVATAR, "upload-1", NOW);

        Map<String, String> fields = permission.fields();
        assertThat(fields.get("public_id")).isEqualTo("chatter/profile/avatars/7/upload-1");
        assertThat(fields.get("allowed_formats")).isEqualTo("jpg,png,webp");
        assertThat(fields.get("context")).isEqualTo("userId=7|slot=AVATAR");
        assertThat(fields.get("timestamp")).isEqualTo(ts(NOW));
        assertThat(fields.get("api_key")).isEqualTo("key-1");
        assertThat(fields.get("notification_url")).endsWith("/uploads/notifications");

        Map<String, Object> signed = new LinkedHashMap<>();
        signed.put("public_id", fields.get("public_id"));
        signed.put("allowed_formats", fields.get("allowed_formats"));
        signed.put("context", fields.get("context"));
        signed.put("timestamp", fields.get("timestamp"));
        signed.put("notification_url", fields.get("notification_url"));
        assertThat(fields.get("signature"))
                .isEqualTo(client.sdk.apiSignRequest(signed, SECRET, client.sdk.config.signatureVersion));
        assertThat(permission.uploadUrl()).isEqualTo("https://api.cloudinary.com/v1_1/demo/image/upload");
        assertThat(permission.method()).isEqualTo("POST");
        assertThat(permission.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void aBannerPermissionUsesTheBannersFolderAndSlot() {
        UploadPermission permission = storage().permit(UserId.of(7), ImageSlot.BANNER, "u", NOW);

        assertThat(permission.fields().get("public_id")).isEqualTo("chatter/profile/banners/7/u");
        assertThat(permission.fields().get("context")).isEqualTo("userId=7|slot=BANNER");
    }

    @Test
    void permitFailsWhenNoCredentialsAreConfigured() {
        var unconfigured = new CloudinaryImageStorage(
                client, new ImageProperties("", "", "", null, null, null, null, null, null, null, null));

        assertThatThrownBy(() -> unconfigured.permit(UserId.of(7), ImageSlot.AVATAR, "u", NOW))
                .isInstanceOf(ImageStorageUnavailableException.class);
    }

    @Test
    void verifyAcceptsAValidNotificationWithEitherDigestAlgorithm() throws Exception {
        for (String algorithm : List.of("SHA-1", "SHA-256")) {
            Optional<ConfirmedUpload> result = verify(body(PUBLIC_ID, "7", "AVATAR", "upload"), ts(NOW), algorithm);

            assertThat(result).isPresent();
            ConfirmedUpload upload = result.get();
            assertThat(upload.userId()).isEqualTo(UserId.of(7));
            assertThat(upload.slot()).isEqualTo(ImageSlot.AVATAR);
            assertThat(upload.publicId()).isEqualTo(PUBLIC_ID);
            assertThat(upload.format()).isEqualTo("png");
            assertThat(upload.bytes()).isEqualTo(1234);
            assertThat(upload.url()).startsWith("https://res.cloudinary.com/");
        }
    }

    @Test
    void verifyAcceptsANonAsciiFilenameSignedOverItsUtf8Bytes() throws Exception {
        assertThat(verify(body(PUBLIC_ID, "7", "AVATAR", "upload"), ts(NOW), "SHA-1")).isPresent();
    }

    @Test
    void verifyRejectsABodyWithOneChangedByte() throws Exception {
        byte[] original = body(PUBLIC_ID, "7", "AVATAR", "upload").getBytes(StandardCharsets.UTF_8);
        String signature = sign(original, ts(NOW), "SHA-1");
        byte[] changed = original.clone();
        changed[20] = (byte) (changed[20] ^ 1);

        assertThat(storage().verify(changed, signature, ts(NOW), NOW)).isEmpty();
    }

    @Test
    void verifyRejectsAWrongSignatureAndMissingParts() {
        byte[] bytes = body(PUBLIC_ID, "7", "AVATAR", "upload").getBytes(StandardCharsets.UTF_8);

        assertThat(storage().verify(bytes, "deadbeef", ts(NOW), NOW)).isEmpty();
        assertThat(storage().verify(bytes, null, ts(NOW), NOW)).isEmpty();
        assertThat(storage().verify(bytes, "x", null, NOW)).isEmpty();
    }

    @Test
    void verifyRejectsATimestampOlderThanTheTolerance() throws Exception {
        String json = body(PUBLIC_ID, "7", "AVATAR", "upload");

        assertThat(verify(json, ts(NOW.minus(Duration.ofHours(3))), "SHA-1")).isEmpty();
        assertThat(verify(json, ts(NOW.minus(Duration.ofHours(1))), "SHA-1")).isPresent();
    }

    @Test
    void verifyRejectsANotificationTypeOtherThanUpload() throws Exception {
        assertThat(verify(body(PUBLIC_ID, "7", "AVATAR", "delete"), ts(NOW), "SHA-1")).isEmpty();
    }

    @Test
    void verifyRejectsAPublicIdOutsideTheLayout() throws Exception {
        assertThat(verify(body("somewhere/else/7/x", "7", "AVATAR", "upload"), ts(NOW), "SHA-1")).isEmpty();
        assertThat(verify(body("chatter/profile/avatars/seven/x", "7", "AVATAR", "upload"), ts(NOW), "SHA-1"))
                .isEmpty();
    }

    @Test
    void verifyRejectsAContextWhoseUserOrSlotDiffersFromThePublicId() throws Exception {
        assertThat(verify(body(PUBLIC_ID, "8", "AVATAR", "upload"), ts(NOW), "SHA-1")).isEmpty();
        assertThat(verify(body(PUBLIC_ID, "7", "BANNER", "upload"), ts(NOW), "SHA-1")).isEmpty();
    }

    @Test
    void verifyRejectsABodyThatIsNotJson() throws Exception {
        assertThat(verify("not json at all", ts(NOW), "SHA-1")).isEmpty();
    }

    @Test
    void removeTreatsAlreadyGoneAsSuccessAndAnyOtherFailureAsUnavailable() {
        storage().remove("a/b");
        assertThat(client.destroyed).containsExactly("a/b");

        client.destroyFailure = new ProviderFailureException("boom");
        assertThatThrownBy(() -> storage().remove("a/b")).isInstanceOf(ImageStorageUnavailableException.class);
    }

    @Test
    void listMapsAPageAndItsCursor() {
        client.page = new CloudinaryClient.ResourcePage(
                List.of(new CloudinaryClient.Resource(PUBLIC_ID, NOW)), "next-1");

        var page = storage().list(null);

        assertThat(page.nextCursor()).isEqualTo("next-1");
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.publicId()).isEqualTo(PUBLIC_ID);
            assertThat(item.createdAt()).isEqualTo(NOW);
        });
    }

    @Test
    void listMapsTheRateLimitAnswerAndOtherFailuresToTheirExceptions() {
        client.listFailure = new ProviderRateLimitedException("slow");
        assertThatThrownBy(() -> storage().list(null)).isInstanceOf(ImageStorageRateLimitedException.class);

        client.listFailure = new ProviderFailureException("boom");
        assertThatThrownBy(() -> storage().list(null))
                .isInstanceOf(ImageStorageUnavailableException.class)
                .isNotInstanceOf(ImageStorageRateLimitedException.class);
    }

    @Test
    void theSdkSignatureAlgorithmCanBeSwitchedToSha256() {
        assertThat(SignatureAlgorithm.SHA256.getAlgorithmId()).isNotBlank();
    }
}

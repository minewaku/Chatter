package com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.storage.r2;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.storage.r2.ObjectStoreClient.PresignedPut;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Signing is pure computation: it needs no storage, so this runs everywhere. */
class SdkObjectStoreClientPresignTest {

    private final SdkObjectStoreClient client = new SdkObjectStoreClient(
            "https://storage.example.test", "auto", true, "access", "secret", Duration.ofSeconds(5), Duration.ofSeconds(3));

    @Test
    void theSignedHeadersFixTheContentTypeAndTheLengthAndHostIsNotReturned() {
        PresignedPut put = client.presignPut("uploads", "42/avatar/abc", "image/webp", 1234, Duration.ofMinutes(15));

        Map<String, String> headers = put.signedHeaders();
        assertThat(headers.keySet().stream().map(String::toLowerCase)).contains("content-type", "content-length");
        assertThat(headers.keySet().stream().map(String::toLowerCase)).doesNotContain("host");
        assertThat(headers.entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase("content-length"))
                        .findFirst()
                        .orElseThrow()
                        .getValue())
                .isEqualTo("1234");
    }

    @Test
    void theAddressTargetsTheGivenBucketAndKeyAndExpiresAsAsked() {
        PresignedPut put = client.presignPut("uploads", "42/avatar/abc", "image/png", 10, Duration.ofMinutes(15));

        assertThat(put.url()).startsWith("https://storage.example.test/uploads/42/avatar/abc?");
        assertThat(put.url()).contains("X-Amz-Expires=900").contains("X-Amz-Signature=");
        assertThat(put.url().toLowerCase()).contains("x-amz-signedheaders=");
        assertThat(put.url()).doesNotContain("secret");
    }

    @Test
    void noChecksumIsEmbeddedInThePresignedAddress() {
        PresignedPut put = client.presignPut("uploads", "k", "image/png", 10, Duration.ofMinutes(15));

        assertThat(put.url().toLowerCase()).doesNotContain("checksum");
        put.signedHeaders().keySet().forEach(name -> assertThat(name.toLowerCase()).doesNotContain("checksum"));
    }

    @Test
    void theUploadCanCreateTheObjectButNotOverwriteOne() {
        PresignedPut put = client.presignPut("uploads", "k", "image/png", 10, Duration.ofMinutes(15));

        assertThat(put.signedHeaders().entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase("if-none-match"))
                        .map(Map.Entry::getValue))
                .containsExactly("*");
    }
}

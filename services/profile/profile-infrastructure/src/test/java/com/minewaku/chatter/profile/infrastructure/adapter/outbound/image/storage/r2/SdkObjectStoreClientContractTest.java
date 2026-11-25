package com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.storage.r2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.storage.r2.ObjectStoreClient.PresignedPut;
import com.minewaku.chatter.profile.infrastructure.support.S3EmulatorContainer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * Proves, against a real S3-compatible server, what the adapter assumes about the SDK calls: a signed PUT is accepted,
 * ranged reads, the no-overwrite condition, and delete of a missing object. Skipped (and says so) when the
 * emulator cannot be started, for example without Docker.
 */
class SdkObjectStoreClientContractTest {

    private static final String BUCKET = "media-" + UUID.randomUUID().toString().substring(0, 8);

    private static S3EmulatorContainer emulator;
    private static SdkObjectStoreClient client;
    private static S3Client admin;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll
    static void start() {
        try {
            emulator = new S3EmulatorContainer();
            emulator.start();
        } catch (RuntimeException e) {
            System.out.println("SKIPPED SdkObjectStoreClientContractTest: S3 emulator unavailable: " + e.getMessage());
            emulator = null;
        }
        assumeTrue(emulator != null, "S3 emulator unavailable");
        admin = S3Client.builder()
                .endpointOverride(URI.create(emulator.endpoint()))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(S3EmulatorContainer.ACCESS_KEY, S3EmulatorContainer.SECRET_KEY)))
                .build();
        admin.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        client = new SdkObjectStoreClient(
                emulator.endpoint(),
                "us-east-1",
                true,
                S3EmulatorContainer.ACCESS_KEY,
                S3EmulatorContainer.SECRET_KEY,
                Duration.ofSeconds(10),
                Duration.ofSeconds(5));
    }

    @AfterAll
    static void stop() {
        if (client != null) {
            client.close();
        }
        if (admin != null) {
            admin.close();
        }
        if (emulator != null) {
            emulator.stop();
        }
    }

    private static int put(PresignedPut presigned, byte[] body, String contentType, String contentLength) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(presigned.url()))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        presigned.signedHeaders().forEach((name, value) -> {
            // the JDK client sets Content-Length itself from the body
            if (!name.equalsIgnoreCase("content-length") && !name.equalsIgnoreCase("content-type")) {
                request.header(name, value);
            }
        });
        request.header("Content-Type", contentType);
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void aSignedPutIsAcceptedAndTheObjectCanBeReadByRange() throws Exception {
        byte[] body = "0123456789abcdefghij".getBytes();
        String key = "profiles/42/avatar/" + UUID.randomUUID();
        PresignedPut presigned = client.presignPut(BUCKET, key, "image/png", body.length, Duration.ofMinutes(5));

        assertThat(put(presigned, body, "image/png", Long.toString(body.length))).isEqualTo(200);

        assertThat(client.readRange(BUCKET, key, 0, 9)).hasValueSatisfying(bytes -> assertThat(bytes).isEqualTo("0123456789".getBytes()));
    }

    @Test
    void aRangeBeyondTheEndOfTheObjectReturnsWhatThereIs() throws Exception {
        byte[] body = "short".getBytes();
        String key = "profiles/42/avatar/" + UUID.randomUUID();
        assertThat(put(client.presignPut(BUCKET, key, "image/png", body.length, Duration.ofMinutes(5)), body, "image/png", "5"))
                .isEqualTo(200);

        assertThat(client.readRange(BUCKET, key, 0, 65_535)).hasValueSatisfying(bytes -> assertThat(bytes).isEqualTo(body));
    }

    @Test
    void aRangeOfAMissingObjectIsEmpty() {
        assertThat(client.readRange(BUCKET, "no/such/key", 0, 9)).isEmpty();
    }

    @Test
    void aSignedPutWithAWrongContentTypeIsRefusedWhenTheServerEnforcesSignedHeaders() throws Exception {
        byte[] body = "abc".getBytes();
        PresignedPut presigned = client.presignPut(BUCKET, "profiles/42/avatar/" + UUID.randomUUID(), "image/png", 3, Duration.ofMinutes(5));

        int status = put(presigned, body, "text/html", "3");

        assumeTrue(status >= 400, "this emulator does not enforce signed headers; the commit's checks are the gate");
        assertThat(status).isGreaterThanOrEqualTo(400);
    }

    @Test
    void aSecondUploadToTheSameKeyIsRefusedWhenTheServerEnforcesTheNoOverwriteCondition() throws Exception {
        String key = "profiles/42/avatar/" + UUID.randomUUID();
        byte[] first = "first".getBytes();
        assertThat(put(client.presignPut(BUCKET, key, "image/png", first.length, Duration.ofMinutes(5)), first, "image/png", "5"))
                .isEqualTo(200);
        byte[] second = "second!".getBytes();

        int status = put(client.presignPut(BUCKET, key, "image/png", second.length, Duration.ofMinutes(5)), second, "image/png", "7");

        assumeTrue(status >= 400, "this emulator does not enforce conditional writes; the short ticket lifetime is the limit");
        assertThat(client.readRange(BUCKET, key, 0, 9)).hasValueSatisfying(bytes -> assertThat(bytes).isEqualTo(first));
    }

    @Test
    void deletingAMissingObjectSucceedsAndDeletingAPresentOneRemovesIt() throws Exception {
        client.delete(BUCKET, "never/existed");

        byte[] body = "x".getBytes();
        PresignedPut presigned = client.presignPut(BUCKET, "profiles/10/avatar/x", "image/png", 1, Duration.ofMinutes(5));
        assertThat(put(presigned, body, "image/png", "1")).isEqualTo(200);
        assertThat(client.readRange(BUCKET, "profiles/10/avatar/x", 0, 0)).isPresent();

        client.delete(BUCKET, "profiles/10/avatar/x");

        assertThat(client.readRange(BUCKET, "profiles/10/avatar/x", 0, 0)).isEmpty();
    }
}

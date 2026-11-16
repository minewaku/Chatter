package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * The AWS SDK v2 implementation of {@link ObjectStoreClient}. Works against any S3-compatible endpoint. Checksums are
 * only calculated when an operation requires them: a presigned PUT cannot carry a checksum of a body that does not exist
 * yet, and several S3-compatible providers reject the optional ones.
 */
public class SdkObjectStoreClient implements ObjectStoreClient, AutoCloseable {

    private final S3Client s3;
    private final S3Presigner presigner;

    public SdkObjectStoreClient(
            String endpoint,
            String region,
            boolean pathStyle,
            String accessKey,
            String secretKey,
            Duration apiCallTimeout,
            Duration apiCallAttemptTimeout) {
        StaticCredentialsProvider credentials =
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        S3Configuration serviceConfiguration =
                S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build();

        var clientBuilder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(serviceConfiguration)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .overrideConfiguration(o -> o.apiCallTimeout(apiCallTimeout).apiCallAttemptTimeout(apiCallAttemptTimeout));
        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(serviceConfiguration);
        if (!endpoint.isBlank()) {
            clientBuilder.endpointOverride(URI.create(endpoint));
            presignerBuilder.endpointOverride(URI.create(endpoint));
        }
        this.s3 = clientBuilder.build();
        this.presigner = presignerBuilder.build();
    }

    @Override
    public PresignedPut presignPut(String bucket, String key, String contentType, long contentLength, Duration validFor) {
        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .ifNoneMatch("*") // a ticket can create its object once; it cannot overwrite what was validated
                    .build();
            PresignedPutObjectRequest presigned =
                    presigner.presignPutObject(r -> r.signatureDuration(validFor).putObjectRequest(put));
            Map<String, String> headers = new LinkedHashMap<>();
            presigned.signedHeaders().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("host")) {
                    headers.put(name, String.join(",", values));
                }
            });
            return new PresignedPut(presigned.url().toString(), headers);
        } catch (RuntimeException e) {
            throw map(e);
        }
    }

    @Override
    public Optional<ObjectHead> head(String bucket, String key) {
        try {
            HeadObjectResponse response =
                    s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return Optional.of(new ObjectHead(response.contentLength(), response.contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw map(e);
        } catch (RuntimeException e) {
            throw map(e);
        }
    }

    @Override
    public byte[] readRange(String bucket, String key, long from, long toInclusive) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .range("bytes=" + from + "-" + toInclusive)
                            .build())
                    .asByteArray();
        } catch (RuntimeException e) {
            throw map(e); // the object vanishing between head and read is reported as unavailable; a retry sorts it out
        }
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (NoSuchKeyException e) {
            // already gone: success
        } catch (RuntimeException e) {
            throw map(e);
        }
    }

    @Override
    public void close() {
        presigner.close();
        s3.close();
    }

    private static ImageStorageUnavailableException map(RuntimeException e) {
        if (e instanceof SdkException) {
            return new ImageStorageUnavailableException("the storage could not be reached or refused the call", e);
        }
        return new ImageStorageUnavailableException("the storage call failed", e);
    }
}

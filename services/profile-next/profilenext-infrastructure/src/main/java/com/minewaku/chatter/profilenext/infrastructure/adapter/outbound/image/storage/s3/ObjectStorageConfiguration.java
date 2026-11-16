package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.application.command.ImagePolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepPolicy;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the storage client and the plain-record policies the core's services are constructed with. */
@Configuration
@EnableConfigurationProperties(ImageStorageProperties.class)
public class ObjectStorageConfiguration {

    /**
     * Without credentials the service still starts, as it does without Vault; every storage call then fails as
     * "unavailable" instead of the bean failing at start-up.
     */
    @Bean
    ObjectStoreClient objectStoreClient(ImageStorageProperties properties) {
        if (!properties.credentialsConfigured()) {
            return new UnconfiguredObjectStoreClient();
        }
        return new SdkObjectStoreClient(
                properties.endpoint(),
                properties.region(),
                properties.pathStyle(),
                properties.accessKey(),
                properties.secretKey(),
                properties.apiCallTimeout(),
                properties.apiCallAttemptTimeout());
    }

    @Bean
    ImagePolicy imagePolicy(ImageStorageProperties properties) {
        Set<String> formats = properties.allowedFormats().stream()
                .map(format -> format.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        return new ImagePolicy(
                properties.maxBytes().avatar().toBytes(),
                properties.maxBytes().banner().toBytes(),
                properties.maxDimension().avatar(),
                properties.maxDimension().banner(),
                formats,
                properties.ticketLifetime(),
                properties.grace(),
                Math.toIntExact(properties.inspectBytes().toBytes()));
    }

    @Bean
    SweepPolicy sweepPolicy(ImageStorageProperties properties) {
        return new SweepPolicy(properties.sweep().batchSize(), properties.sweep().retryDelay());
    }

    /** Stands in while no storage credentials are configured. */
    static final class UnconfiguredObjectStoreClient implements ObjectStoreClient {

        private static ImageStorageUnavailableException unavailable() {
            return new ImageStorageUnavailableException("image storage is not configured");
        }

        @Override
        public PresignedPut presignPut(String bucket, String key, String contentType, long contentLength, Duration validFor) {
            throw unavailable();
        }

        @Override
        public Optional<ObjectHead> head(String bucket, String key) {
            throw unavailable();
        }

        @Override
        public byte[] readRange(String bucket, String key, long from, long toInclusive) {
            throw unavailable();
        }

        @Override
        public void delete(String bucket, String key) {
            throw unavailable();
        }
    }
}

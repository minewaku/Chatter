package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.r2;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the client that talks to Cloudflare R2 through its S3-compatible API. */
@Configuration
@EnableConfigurationProperties(R2Properties.class)
public class R2Configuration {

    /**
     * Without credentials the service still starts, as it does without Vault; every storage call then fails as
     * "unavailable" instead of the bean failing at start-up. R2 serves the bucket under the path
     * ({@code https://<account>.r2.cloudflarestorage.com/<bucket>/<key>}), so path-style addressing is fixed.
     */
    @Bean(destroyMethod = "close")
    ObjectStoreClient objectStoreClient(R2Properties properties) {
        if (!properties.credentialsConfigured()) {
            return new UnconfiguredObjectStoreClient();
        }
        return new SdkObjectStoreClient(
                properties.endpoint(),
                properties.region(),
                true,
                properties.accessKey(),
                properties.secretKey(),
                properties.apiCallTimeout(),
                properties.apiCallAttemptTimeout());
    }

    /** Stands in while no R2 credentials are configured. */
    static final class UnconfiguredObjectStoreClient implements ObjectStoreClient, AutoCloseable {

        private static ImageStorageUnavailableException unavailable() {
            return new ImageStorageUnavailableException("image storage is not configured");
        }

        @Override
        public PresignedPut presignPut(String bucket, String key, String contentType, long contentLength, Duration validFor) {
            throw unavailable();
        }

        @Override
        public Optional<byte[]> readRange(String bucket, String key, long from, long toInclusive) {
            throw unavailable();
        }

        @Override
        public void delete(String bucket, String key) {
            throw unavailable();
        }

        @Override
        public void close() {}
    }
}

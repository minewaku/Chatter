package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.SignatureAlgorithm;
import com.cloudinary.utils.ObjectUtils;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.application.command.ImagePolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepPolicy;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the Cloudinary client and the plain-record policies the core's services are constructed with. */
@Configuration
@EnableConfigurationProperties(ImageStorageProperties.class)
public class CloudinaryConfiguration {

    /**
     * Without credentials the service still starts, as it does without Vault; every storage call then fails as
     * "unavailable" instead of the bean failing at start-up.
     */
    @Bean
    CloudinaryClient cloudinaryClient(ImageStorageProperties properties) {
        if (!properties.credentialsConfigured()) {
            return new UnconfiguredCloudinaryClient();
        }
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.cloudName(),
                "api_key", properties.apiKey(),
                "api_secret", properties.apiSecret(),
                "secure", true));
        cloudinary.config.signatureAlgorithm =
                properties.signatureAlgorithm().equals("SHA-256") ? SignatureAlgorithm.SHA256 : SignatureAlgorithm.SHA1;
        return new SdkCloudinaryClient(cloudinary, properties.deliveryUrl(), properties.apiCallTimeout());
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

    /** Stands in while no Cloudinary credentials are configured. */
    static final class UnconfiguredCloudinaryClient implements CloudinaryClient {

        private static ImageStorageUnavailableException unavailable() {
            return new ImageStorageUnavailableException("image storage is not configured");
        }

        @Override
        public String sign(java.util.Map<String, Object> params) {
            throw unavailable();
        }

        @Override
        public Optional<Asset> asset(String publicId) {
            throw unavailable();
        }

        @Override
        public byte[] readLeadingBytes(String publicId, int maxBytes) {
            throw unavailable();
        }

        @Override
        public void destroy(String publicId) {
            throw unavailable();
        }
    }
}

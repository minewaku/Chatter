package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Image settings ({@code profile-next.images.*}). The two storage credentials arrive through placeholders that read
 * Vault; {@link #toString()} leaves them out so a property dump never shows them.
 */
@ConfigurationProperties("profile-next.images")
public record ImageStorageProperties(
        String endpoint,
        String region,
        Boolean pathStyle,
        String accessKey,
        String secretKey,
        String bucket,
        String publicBaseUrl,
        Duration ticketLifetime,
        Duration grace,
        MaxBytes maxBytes,
        MaxDimension maxDimension,
        List<String> allowedFormats,
        DataSize inspectBytes,
        Duration apiCallTimeout,
        Duration apiCallAttemptTimeout,
        Sweep sweep) {

    public ImageStorageProperties {
        endpoint = endpoint == null ? "" : endpoint.strip();
        region = region == null || region.isBlank() ? "auto" : region.strip();
        pathStyle = pathStyle != null && pathStyle;
        accessKey = accessKey == null ? "" : accessKey;
        secretKey = secretKey == null ? "" : secretKey;
        bucket = bucket == null ? "" : bucket.strip();
        publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.strip();
        ticketLifetime = ticketLifetime == null ? Duration.ofMinutes(5) : ticketLifetime;
        grace = grace == null ? Duration.ofMinutes(10) : grace;
        maxBytes = maxBytes == null ? new MaxBytes(null, null) : maxBytes;
        maxDimension = maxDimension == null ? new MaxDimension(null, null) : maxDimension;
        allowedFormats =
                allowedFormats == null || allowedFormats.isEmpty() ? List.of("jpg", "png", "webp") : allowedFormats;
        inspectBytes = inspectBytes == null ? DataSize.ofKilobytes(512) : inspectBytes;
        apiCallTimeout = apiCallTimeout == null ? Duration.ofSeconds(5) : apiCallTimeout;
        apiCallAttemptTimeout = apiCallAttemptTimeout == null ? Duration.ofSeconds(3) : apiCallAttemptTimeout;
        sweep = sweep == null ? new Sweep(null, null) : sweep;
    }

    /** True when the credentials and the bucket name are set; without them every storage call is "unavailable". */
    public boolean credentialsConfigured() {
        return !accessKey.isBlank() && !secretKey.isBlank() && !bucket.isBlank();
    }

    public record MaxBytes(DataSize avatar, DataSize banner) {

        public MaxBytes {
            avatar = avatar == null ? DataSize.ofMegabytes(5) : avatar;
            banner = banner == null ? DataSize.ofMegabytes(10) : banner;
        }
    }

    public record MaxDimension(Integer avatar, Integer banner) {

        public MaxDimension {
            avatar = avatar == null ? 1024 : avatar;
            banner = banner == null ? 2048 : banner;
        }
    }

    /** {@code batchSize}: keys handled per sweep run. {@code retryDelay}: wait before a failed delete is tried again. */
    public record Sweep(Integer batchSize, Duration retryDelay) {

        public Sweep {
            batchSize = batchSize == null ? 100 : batchSize;
            retryDelay = retryDelay == null ? Duration.ofMinutes(5) : retryDelay;
        }
    }

    @Override
    public String toString() {
        return "ImageStorageProperties[endpoint=" + endpoint + ", region=" + region + ", bucket=" + bucket
                + ", credentialsConfigured=" + credentialsConfigured() + "]";
    }
}

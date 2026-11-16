package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Image settings ({@code profile-next.images.*}). The three Cloudinary credentials arrive through placeholders that
 * read Vault; {@link #toString()} leaves them out so a property dump never shows them.
 */
@ConfigurationProperties("profile-next.images")
public record ImageStorageProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String signatureAlgorithm,
        String deliveryUrl,
        Duration ticketLifetime,
        Duration grace,
        MaxBytes maxBytes,
        MaxDimension maxDimension,
        List<String> allowedFormats,
        DataSize inspectBytes,
        Duration apiCallTimeout,
        Sweep sweep) {

    public ImageStorageProperties {
        cloudName = cloudName == null ? "" : cloudName.strip();
        apiKey = apiKey == null ? "" : apiKey.strip();
        apiSecret = apiSecret == null ? "" : apiSecret;
        if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
            signatureAlgorithm = "SHA-1";
        }
        if (!signatureAlgorithm.equals("SHA-1") && !signatureAlgorithm.equals("SHA-256")) {
            throw new IllegalArgumentException("profile-next.images.signature-algorithm must be SHA-1 or SHA-256");
        }
        deliveryUrl = deliveryUrl == null || deliveryUrl.isBlank()
                ? "https://res.cloudinary.com/" + cloudName + "/image/upload"
                : stripTrailingSlashes(deliveryUrl.strip());
        ticketLifetime = ticketLifetime == null ? Duration.ofMinutes(5) : ticketLifetime;
        grace = grace == null ? Duration.ofMinutes(10) : grace;
        maxBytes = maxBytes == null ? new MaxBytes(null, null) : maxBytes;
        maxDimension = maxDimension == null ? new MaxDimension(null, null) : maxDimension;
        allowedFormats =
                allowedFormats == null || allowedFormats.isEmpty() ? List.of("jpg", "png", "webp") : allowedFormats;
        inspectBytes = inspectBytes == null ? DataSize.ofKilobytes(512) : inspectBytes;
        apiCallTimeout = apiCallTimeout == null ? Duration.ofSeconds(5) : apiCallTimeout;
        sweep = sweep == null ? new Sweep(null, null) : sweep;
    }

    /** True when all three credentials are set; without them every storage call is "unavailable". */
    public boolean credentialsConfigured() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    private static String stripTrailingSlashes(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
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
        return "ImageStorageProperties[cloudName=" + cloudName + ", deliveryUrl=" + deliveryUrl
                + ", credentialsConfigured=" + credentialsConfigured() + "]";
    }
}

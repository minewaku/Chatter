package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Image settings (contracts/image-storage.md, Configuration). The three provider credentials arrive through
 * placeholders that read Vault; {@link #toString()} leaves them out so a property dump never shows them.
 */
@ConfigurationProperties("profile-next.images")
public record ImageProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String webhookUrl,
        String signatureAlgorithm,
        Duration permissionLifetime,
        Duration webhookTolerance,
        MaxBytes maxBytes,
        List<String> allowedFormats,
        Removal removal,
        Sweep sweep) {

    public ImageProperties {
        cloudName = cloudName == null ? "" : cloudName;
        apiKey = apiKey == null ? "" : apiKey;
        apiSecret = apiSecret == null ? "" : apiSecret;
        webhookUrl = webhookUrl == null ? "" : webhookUrl;
        if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
            signatureAlgorithm = "SHA-1";
        }
        if (!signatureAlgorithm.equals("SHA-1") && !signatureAlgorithm.equals("SHA-256")) {
            throw new IllegalArgumentException("profile-next.images.signature-algorithm must be SHA-1 or SHA-256");
        }
        permissionLifetime = permissionLifetime == null ? Duration.ofMinutes(15) : permissionLifetime;
        webhookTolerance = webhookTolerance == null ? Duration.ofHours(2) : webhookTolerance;
        maxBytes = maxBytes == null ? new MaxBytes(null, null) : maxBytes;
        allowedFormats = allowedFormats == null || allowedFormats.isEmpty() ? List.of("jpg", "png", "webp") : allowedFormats;
        removal = removal == null ? new Removal(null, null, null, null, null) : removal;
        sweep = sweep == null ? new Sweep(null, null) : sweep;
    }

    public boolean credentialsConfigured() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    public record MaxBytes(DataSize avatar, DataSize banner) {

        public MaxBytes {
            avatar = avatar == null ? DataSize.ofMegabytes(5) : avatar;
            banner = banner == null ? DataSize.ofMegabytes(10) : banner;
        }
    }

    public record Removal(
            Integer maxRetries, List<Duration> delays, Duration pollInterval, Integer pollBatchSize, Duration lease) {

        public Removal {
            maxRetries = maxRetries == null ? 10 : maxRetries;
            delays = delays == null || delays.isEmpty()
                    ? List.of(
                            Duration.ofMinutes(2),
                            Duration.ofMinutes(4),
                            Duration.ofMinutes(8),
                            Duration.ofMinutes(16),
                            Duration.ofMinutes(32),
                            Duration.ofHours(1))
                    : delays;
            pollInterval = pollInterval == null ? Duration.ofSeconds(30) : pollInterval;
            pollBatchSize = pollBatchSize == null ? 50 : pollBatchSize;
            lease = lease == null ? Duration.ofMinutes(5) : lease;
        }
    }

    public record Sweep(Duration gracePeriod, Duration interval) {

        public Sweep {
            gracePeriod = gracePeriod == null ? Duration.ofHours(24) : gracePeriod;
            interval = interval == null ? Duration.ofHours(1) : interval;
        }
    }

    @Override
    public String toString() {
        return "ImageProperties[webhookUrl=" + webhookUrl + ", signatureAlgorithm=" + signatureAlgorithm
                + ", credentialsConfigured=" + credentialsConfigured() + "]";
    }
}

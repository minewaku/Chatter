package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Provider-neutral image settings ({@code profile-next.images.*}): what an upload may be, how long a ticket lives and how
 * the sweeper behaves. Where the files are stored is the storage adapter's own configuration.
 */
@ConfigurationProperties("profile-next.images")
public record ImageProperties(
        Duration ticketLifetime,
        Duration grace,
        MaxBytes maxBytes,
        MaxDimension maxDimension,
        List<String> allowedFormats,
        DataSize inspectBytes,
        Sweep sweep) {

    public ImageProperties {
        ticketLifetime = ticketLifetime == null ? Duration.ofMinutes(5) : ticketLifetime;
        grace = grace == null ? Duration.ofMinutes(10) : grace;
        maxBytes = maxBytes == null ? new MaxBytes(null, null) : maxBytes;
        maxDimension = maxDimension == null ? new MaxDimension(null, null) : maxDimension;
        allowedFormats =
                allowedFormats == null || allowedFormats.isEmpty() ? List.of("jpg", "png", "webp") : allowedFormats;
        inspectBytes = inspectBytes == null ? DataSize.ofKilobytes(512) : inspectBytes;
        sweep = sweep == null ? new Sweep(null, null) : sweep;
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
}

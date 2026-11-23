package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.r2;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloudflare R2 settings ({@code profile-next.images.r2.*}). The two keys arrive through placeholders that read Vault;
 * {@link #toString()} leaves them out so a property dump never shows them. Either {@code accountId} or a full
 * {@code endpoint} names the account; {@code endpoint} wins when both are set (an emulator, or an EU jurisdiction
 * address).
 *
 * @param region R2 only accepts {@code auto} (and its aliases), which is the default
 */
@ConfigurationProperties("profile-next.images.r2")
public record R2Properties(
        String accountId,
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        Duration apiCallTimeout,
        Duration apiCallAttemptTimeout) {

    public R2Properties {
        accountId = accountId == null ? "" : accountId.strip();
        endpoint = endpoint == null ? "" : stripTrailingSlashes(endpoint.strip());
        if (endpoint.isEmpty() && !accountId.isEmpty()) {
            endpoint = "https://" + accountId + ".r2.cloudflarestorage.com";
        }
        region = region == null || region.isBlank() ? "auto" : region.strip();
        bucket = bucket == null ? "" : bucket.strip();
        accessKey = accessKey == null ? "" : accessKey.strip();
        secretKey = secretKey == null ? "" : secretKey.strip();
        apiCallTimeout = apiCallTimeout == null ? Duration.ofSeconds(5) : apiCallTimeout;
        apiCallAttemptTimeout = apiCallAttemptTimeout == null ? Duration.ofSeconds(3) : apiCallAttemptTimeout;
    }

    /** True when the account, the bucket and both keys are set; without them every storage call is "unavailable". */
    public boolean credentialsConfigured() {
        return !endpoint.isEmpty() && !bucket.isEmpty() && !accessKey.isEmpty() && !secretKey.isEmpty();
    }

    private static String stripTrailingSlashes(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @Override
    public String toString() {
        return "R2Properties[endpoint=" + endpoint + ", bucket=" + bucket + ", credentialsConfigured="
                + credentialsConfigured() + "]";
    }
}

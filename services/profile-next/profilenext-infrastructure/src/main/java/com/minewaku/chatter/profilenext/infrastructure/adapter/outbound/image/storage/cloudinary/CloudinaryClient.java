package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import java.util.Map;
import java.util.Optional;

/**
 * The only place that talks to Cloudinary, and the seam between the storage adapter and the SDK. Every provider or
 * connection failure surfaces as {@code ImageStorageUnavailableException}; an SDK exception never crosses this interface.
 */
public interface CloudinaryClient {

    /** Signs upload parameters with the account secret, exactly as the SDK does. Makes no network call. */
    String sign(Map<String, Object> params);

    /** What Cloudinary holds under a public id, or empty when nothing is there. */
    Optional<Asset> asset(String publicId);

    /** The first {@code maxBytes} bytes of the stored original (all of it when it is shorter). */
    byte[] readLeadingBytes(String publicId, int maxBytes);

    /** Deletes an asset and invalidates its CDN copies. An asset that is already gone counts as success. */
    void destroy(String publicId);

    /** {@code format} is Cloudinary's short name for the stored file ({@code jpg}, {@code png}, {@code webp}...). */
    record Asset(long bytes, String format) {}
}

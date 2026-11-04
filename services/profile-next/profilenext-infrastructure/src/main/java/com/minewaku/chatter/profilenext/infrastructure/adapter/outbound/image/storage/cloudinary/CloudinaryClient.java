package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** The three SDK calls the adapter needs, so the adapter can be tested without the provider. */
interface CloudinaryClient {

    /** Signs upload parameters with the account secret, exactly as the SDK does. */
    String sign(Map<String, Object> params);

    /** Deletes an image. Returns normally when the image is already gone; throws {@link ProviderFailureException}. */
    void destroy(String publicId);

    /** One page of the listing under a prefix; may throw {@link ProviderRateLimitedException}. */
    ResourcePage listResources(String prefix, String cursor);

    record Resource(String publicId, Instant createdAt) {}

    record ResourcePage(List<Resource> resources, String nextCursor) {}
}

package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * The only place that knows buckets and keys, and the seam between the storage adapter and the SDK. Every provider or
 * connection failure surfaces as {@code ImageStorageUnavailableException}; an SDK exception never crosses this interface.
 */
public interface ObjectStoreClient {

    /**
     * Signs a PUT for one object, fixing its content type and length and allowing it to create the object only, never
     * to overwrite one. Makes no network call.
     */
    PresignedPut presignPut(String bucket, String key, String contentType, long contentLength, Duration validFor);

    /** The object's length and recorded content type, or empty when it does not exist. */
    Optional<ObjectHead> head(String bucket, String key);

    /** Bytes {@code from} to {@code toInclusive} of an object (fewer when the object ends sooner). */
    byte[] readRange(String bucket, String key, long from, long toInclusive);

    /** Deletes an object. A missing object counts as success. */
    void delete(String bucket, String key);

    /** A signed address and the headers the client has to send with it (without {@code Host}). */
    record PresignedPut(String url, Map<String, String> signedHeaders) {}

    record ObjectHead(long contentLength, String contentType) {}
}

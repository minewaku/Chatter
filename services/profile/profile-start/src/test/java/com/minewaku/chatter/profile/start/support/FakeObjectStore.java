package com.minewaku.chatter.profile.start.support;

import com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.storage.r2.ObjectStoreClient;
import com.minewaku.chatter.profile.profile.api.exception.ImageStorageUnavailableException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory object store that stands in for the SDK client in end-to-end tests, so the real adapter, use cases and
 * database run unchanged. "The client uploads" is the test calling {@link #putViaUrl} with the address the service
 * handed out.
 */
public class FakeObjectStore implements ObjectStoreClient {

    public record Stored(byte[] bytes, String contentType) {}

    private static final String BASE = "https://fake-storage.test/";

    private final Map<String, Stored> objects = new ConcurrentHashMap<>();

    /** Fail every call with "unavailable" while true. */
    public volatile boolean failing;

    /** Fail only deletes while true. */
    public volatile boolean failingDeletes;

    public void reset() {
        objects.clear();
        failing = false;
        failingDeletes = false;
    }

    /** The client's PUT: the address names bucket and key; the bytes become the object. */
    public void putViaUrl(String url, byte[] bytes, String contentType) {
        String path = url.substring(BASE.length());
        int query = path.indexOf('?');
        objects.put(query < 0 ? path : path.substring(0, query), new Stored(bytes, contentType));
    }

    /** The keys currently in a bucket, sorted. */
    public Set<String> keys(String bucket) {
        Set<String> keys = new TreeSet<>();
        String prefix = bucket + "/";
        objects.keySet().stream().filter(k -> k.startsWith(prefix)).forEach(k -> keys.add(k.substring(prefix.length())));
        return keys;
    }

    public Stored get(String bucket, String key) {
        return objects.get(bucket + "/" + key);
    }

    private void checkAvailable() {
        if (failing) {
            throw new ImageStorageUnavailableException("storage is down");
        }
    }

    @Override
    public PresignedPut presignPut(String bucket, String key, String contentType, long contentLength, Duration validFor) {
        checkAvailable();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("content-type", contentType);
        headers.put("content-length", Long.toString(contentLength));
        headers.put("if-none-match", "*");
        return new PresignedPut(BASE + bucket + "/" + key + "?X-Amz-Expires=" + validFor.toSeconds(), headers);
    }

    @Override
    public Optional<byte[]> readRange(String bucket, String key, long from, long toInclusive) {
        checkAvailable();
        Stored stored = objects.get(bucket + "/" + key);
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(Arrays.copyOfRange(stored.bytes(), (int) from, (int) Math.min(toInclusive + 1, stored.bytes().length)));
    }

    @Override
    public void delete(String bucket, String key) {
        checkAvailable();
        if (failingDeletes) {
            throw new ImageStorageUnavailableException("delete failed");
        }
        objects.remove(bucket + "/" + key);
    }
}

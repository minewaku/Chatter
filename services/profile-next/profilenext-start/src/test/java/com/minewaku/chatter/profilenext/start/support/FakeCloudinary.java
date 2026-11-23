package com.minewaku.chatter.profilenext.start.support;

import com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary.CloudinaryClient;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory Cloudinary that stands in for the SDK client in end-to-end tests, so the real adapter, use cases and
 * database run unchanged. "The client uploads" is the test calling {@link #upload} with the {@code public_id} the service
 * put in the ticket's fields.
 */
public class FakeCloudinary implements CloudinaryClient {

    public record Stored(byte[] bytes, String contentType) {}

    private final Map<String, Stored> assets = new ConcurrentHashMap<>();

    /** Fail every call with "unavailable" while true. */
    public volatile boolean failing;

    /** Fail only deletes while true. */
    public volatile boolean failingDeletes;

    public void reset() {
        assets.clear();
        failing = false;
        failingDeletes = false;
    }

    /** The client's form POST: the public id names the asset; the bytes become it. */
    public void upload(String publicId, byte[] bytes, String contentType) {
        assets.put(publicId, new Stored(bytes, contentType));
    }

    /** The public ids currently stored, sorted. */
    public Set<String> publicIds() {
        return new TreeSet<>(assets.keySet());
    }

    public Stored get(String publicId) {
        return assets.get(publicId);
    }

    private void checkAvailable() {
        if (failing) {
            throw new ImageStorageUnavailableException("storage is down");
        }
    }

    @Override
    public String sign(Map<String, Object> params) {
        checkAvailable();
        return "fake-signature";
    }

    @Override
    public Optional<Asset> asset(String publicId) {
        checkAvailable();
        Stored stored = assets.get(publicId);
        return stored == null ? Optional.empty() : Optional.of(new Asset(stored.bytes().length, formatOf(stored.contentType())));
    }

    @Override
    public byte[] readLeadingBytes(String publicId, int maxBytes) {
        checkAvailable();
        Stored stored = assets.get(publicId);
        if (stored == null) {
            throw new ImageStorageUnavailableException("asset vanished");
        }
        return Arrays.copyOf(stored.bytes(), Math.min(maxBytes, stored.bytes().length));
    }

    @Override
    public void destroy(String publicId) {
        checkAvailable();
        if (failingDeletes) {
            throw new ImageStorageUnavailableException("delete failed");
        }
        assets.remove(publicId);
    }

    private static String formatOf(String contentType) {
        return "image/jpeg".equals(contentType) ? "jpg" : contentType.substring(contentType.indexOf('/') + 1);
    }
}

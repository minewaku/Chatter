package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.model.ImagePath;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** An in-memory object store: what a client "uploaded" is put straight into {@link #objects}. */
public class FakeImageStorage implements ImageStorage {

    public record Stored(byte[] bytes, String contentType) {}

    public final Map<OwnedImage, Stored> objects = new LinkedHashMap<>();

    /** Calls in order: {@code issue:<path>}, {@code stat:<path>}, {@code header:<path>}, {@code delete:<path>}. */
    public final List<String> calls = new ArrayList<>();

    public boolean validatesOnUpload;
    public boolean failDeletes;
    public boolean failStat;
    public Duration lastValidFor;
    public String lastContentType;
    public long lastSizeBytes;

    private final Instant now = Instant.parse("2026-11-04T10:00:00Z");

    /** Simulates a client finishing an upload to the ticket's location. */
    public void upload(OwnedImage image, byte[] bytes, String contentType) {
        objects.put(image, new Stored(bytes, contentType));
    }

    @Override
    public UploadTicket issueUpload(OwnedImage image, String contentType, long sizeBytes, Duration validFor) {
        calls.add("issue:" + ImagePath.of(image));
        lastValidFor = validFor;
        lastContentType = contentType;
        lastSizeBytes = sizeBytes;
        return new UploadTicket(
                "https://storage.test/" + ImagePath.of(image),
                "PUT",
                Map.of("Content-Type", contentType),
                Map.of(),
                now.plus(validFor));
    }

    @Override
    public Optional<StoredObject> stat(OwnedImage image) {
        calls.add("stat:" + ImagePath.of(image));
        if (failStat) {
            throw new ImageStorageUnavailableException("stat failed");
        }
        Stored stored = objects.get(image);
        return stored == null ? Optional.empty() : Optional.of(new StoredObject(stored.bytes().length, stored.contentType()));
    }

    @Override
    public byte[] readHeader(OwnedImage image, int maxBytes) {
        calls.add("header:" + ImagePath.of(image));
        Stored stored = objects.get(image);
        if (stored == null) {
            throw new ImageStorageUnavailableException("nothing stored");
        }
        return Arrays.copyOf(stored.bytes(), Math.min(maxBytes, stored.bytes().length));
    }

    @Override
    public void delete(OwnedImage image) {
        calls.add("delete:" + ImagePath.of(image));
        if (failDeletes) {
            throw new ImageStorageUnavailableException("delete failed");
        }
        objects.remove(image);
    }

    @Override
    public boolean validatesOnUpload() {
        return validatesOnUpload;
    }
}

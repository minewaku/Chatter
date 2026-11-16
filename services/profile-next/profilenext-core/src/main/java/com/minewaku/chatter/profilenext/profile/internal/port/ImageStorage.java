package com.minewaku.chatter.profilenext.profile.internal.port;

import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An object storage provider, seen from core: four operations every provider has. No method exposes a bucket, a key or a
 * provider type; core names an image by owner, slot and id and the adapter alone knows where that lives. Provider
 * failures surface as {@code ImageStorageUnavailableException}.
 */
public interface ImageStorage {

    /**
     * Signs an upload of one file to the location of exactly this image. The client sends it straight to the provider.
     * Constraints (content type, length, no overwrite) are applied wherever the provider can enforce them; the commit
     * checks the stored result either way. Makes no change in storage.
     */
    UploadTicket issueUpload(OwnedImage image, String contentType, long sizeBytes, Duration validFor);

    /** What is stored for the image, or empty when nothing has arrived. */
    Optional<StoredObject> stat(OwnedImage image);

    /** The first {@code maxBytes} bytes of the stored file (all of it when it is shorter). Fails when nothing is stored. */
    byte[] readHeader(OwnedImage image, int maxBytes);

    /** Deletes the stored file. A file that is already gone counts as success. */
    void delete(OwnedImage image);

    /**
     * True when the provider itself validates and re-encodes what it receives (an image CDN), which makes reading the
     * header ourselves redundant. Raw object stores answer false.
     */
    boolean validatesOnUpload();

    /**
     * What a client needs to upload one file: send {@code method} to {@code url} with {@code headers} (and, for form
     * uploads, {@code fields} plus the file as multipart form data).
     */
    record UploadTicket(String url, String method, Map<String, String> headers, Map<String, String> fields, Instant expiresAt) {

        public UploadTicket {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(expiresAt, "expiresAt");
            headers = Map.copyOf(headers);
            fields = Map.copyOf(fields);
        }
    }

    /** A stored file as the provider reports it. {@code contentType} is what the provider recorded, never what a client claimed later. */
    record StoredObject(long sizeBytes, String contentType) {}
}

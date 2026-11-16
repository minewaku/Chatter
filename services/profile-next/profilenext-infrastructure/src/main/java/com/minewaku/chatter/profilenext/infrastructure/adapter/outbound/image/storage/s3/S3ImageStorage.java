package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3;

import com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3.ObjectStoreClient.ObjectHead;
import com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.s3.ObjectStoreClient.PresignedPut;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.model.ImagePath;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * The adapter for {@link ImageStorage} on any S3-compatible store. Clients upload straight to the bucket with a signed
 * PUT at the key {@link ImagePath} names; nothing is ever copied or renamed, which is what lets other providers (some of
 * which have no copy) implement the same port. Everything bucket-shaped is decided here and the SDK stays behind
 * {@link ObjectStoreClient}.
 */
@Component
public class S3ImageStorage implements ImageStorage {

    private final ObjectStoreClient client;
    private final ImageStorageProperties properties;
    private final Clock clock;

    S3ImageStorage(ObjectStoreClient client, ImageStorageProperties properties, Clock clock) {
        this.client = client;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public UploadTicket issueUpload(OwnedImage image, String contentType, long sizeBytes, Duration validFor) {
        requireConfigured();
        PresignedPut presigned = guarded(
                "the upload could not be signed",
                () -> client.presignPut(properties.bucket(), key(image), contentType, sizeBytes, validFor));
        // exactly what the client must send: the signed type and length plus every other signed header
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Content-Length", Long.toString(sizeBytes));
        presigned.signedHeaders().forEach((name, value) -> {
            if (!name.equalsIgnoreCase("host")
                    && !name.equalsIgnoreCase("content-type")
                    && !name.equalsIgnoreCase("content-length")) {
                headers.put(name, value);
            }
        });
        return new UploadTicket(presigned.url(), "PUT", headers, Map.of(), clock.instant().plus(validFor));
    }

    @Override
    public Optional<StoredObject> stat(OwnedImage image) {
        requireConfigured();
        return guarded("the upload could not be read", () -> client.head(properties.bucket(), key(image))
                .map(head -> new StoredObject(head.contentLength(), head.contentType())));
    }

    @Override
    public byte[] readHeader(OwnedImage image, int maxBytes) {
        requireConfigured();
        return guarded("the upload could not be read", () -> client.readRange(properties.bucket(), key(image), 0, maxBytes - 1L));
    }

    @Override
    public void delete(OwnedImage image) {
        requireConfigured();
        guarded("the image could not be deleted", () -> {
            client.delete(properties.bucket(), key(image));
            return Boolean.TRUE;
        });
    }

    /** A raw object store keeps whatever bytes it was given, so the service reads the header itself. */
    @Override
    public boolean validatesOnUpload() {
        return false;
    }

    private static String key(OwnedImage image) {
        return ImagePath.of(image);
    }

    private void requireConfigured() {
        if (!properties.credentialsConfigured()) {
            throw new ImageStorageUnavailableException("image storage is not configured");
        }
    }

    /** Lets the core storage exception through and turns anything else the client threw into "unavailable". */
    private static <T> T guarded(String failure, Supplier<T> call) {
        try {
            return call.get();
        } catch (ImageStorageUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ImageStorageUnavailableException(failure, e);
        }
    }
}

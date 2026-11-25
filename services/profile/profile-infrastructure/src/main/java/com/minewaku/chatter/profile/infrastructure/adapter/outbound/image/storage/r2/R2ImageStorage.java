package com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.storage.r2;

import com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.storage.r2.ObjectStoreClient.PresignedPut;
import com.minewaku.chatter.profile.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profile.profile.internal.model.ImagePath;
import com.minewaku.chatter.profile.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profile.profile.internal.port.ImageStorage;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * The adapter for {@link ImageStorage} on Cloudflare R2. The client uploads straight to R2 with a presigned PUT whose
 * object key is the path {@link ImagePath} names. The signature fixes the content type and the length the ticket was
 * issued for, and the request carries {@code If-None-Match: *}, so a ticket can create its object once and never
 * overwrite one; nothing is ever copied or renamed. R2 has no form (POST policy) upload, so the ticket has no
 * {@code fields}. Everything bucket-shaped is decided here and the SDK stays behind {@link ObjectStoreClient}.
 */
@Component
public class R2ImageStorage implements ImageStorage {

    private final ObjectStoreClient client;
    private final R2Properties properties;
    private final Clock clock;

    R2ImageStorage(ObjectStoreClient client, R2Properties properties, Clock clock) {
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

    /** One ranged read: a missing object is the empty answer, which is how the commit learns the upload is not there. */
    @Override
    public Optional<byte[]> readFirstBytes(OwnedImage image, int maxBytes) {
        requireConfigured();
        return guarded(
                "the upload could not be read",
                () -> client.readRange(properties.bucket(), key(image), 0, maxBytes - 1L));
    }

    @Override
    public void delete(OwnedImage image) {
        requireConfigured();
        guarded("the image could not be deleted", () -> {
            client.delete(properties.bucket(), key(image));
            return Boolean.TRUE;
        });
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

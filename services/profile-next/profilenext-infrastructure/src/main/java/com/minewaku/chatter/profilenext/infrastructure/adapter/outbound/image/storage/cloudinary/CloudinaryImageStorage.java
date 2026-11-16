package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.model.ImagePath;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * The adapter for {@link ImageStorage} on Cloudinary. Clients upload straight to Cloudinary with a signed form POST whose
 * {@code public_id} is the path {@link ImagePath} names and which may create that asset only, never overwrite one;
 * nothing is ever copied or renamed. Cloudinary cannot pin a file's length or content type in the signature, so the
 * service checks both after the upload: the stored size and type in {@link #stat}, and the file's own first bytes through
 * {@link #readHeader}, which is why {@link #validatesOnUpload()} stays false. Everything Cloudinary-shaped is decided here
 * and the SDK stays behind {@link CloudinaryClient}.
 */
@Component
public class CloudinaryImageStorage implements ImageStorage {

    private final CloudinaryClient client;
    private final ImageStorageProperties properties;
    private final Clock clock;

    CloudinaryImageStorage(CloudinaryClient client, ImageStorageProperties properties, Clock clock) {
        this.client = client;
        this.properties = properties;
        this.clock = clock;
    }

    /** {@code contentType} and {@code sizeBytes} cannot be part of a Cloudinary signature; the commit checks them instead. */
    @Override
    public UploadTicket issueUpload(OwnedImage image, String contentType, long sizeBytes, Duration validFor) {
        requireConfigured();
        Map<String, Object> signed = new LinkedHashMap<>();
        signed.put("public_id", publicId(image));
        signed.put("allowed_formats", String.join(",", properties.allowedFormats()));
        signed.put("overwrite", "false");
        signed.put("timestamp", Long.toString(clock.instant().getEpochSecond()));
        String signature = guarded("the upload could not be signed", () -> client.sign(signed));

        Map<String, String> fields = new LinkedHashMap<>();
        signed.forEach((name, value) -> fields.put(name, value.toString()));
        fields.put("api_key", properties.apiKey());
        fields.put("signature", signature);
        return new UploadTicket(
                "https://api.cloudinary.com/v1_1/" + properties.cloudName() + "/image/upload",
                "POST",
                Map.of(),
                fields,
                clock.instant().plus(validFor));
    }

    @Override
    public Optional<StoredObject> stat(OwnedImage image) {
        requireConfigured();
        return guarded("the upload could not be read", () -> client.asset(publicId(image))
                .map(asset -> new StoredObject(asset.bytes(), contentTypeOf(asset.format()))));
    }

    @Override
    public byte[] readHeader(OwnedImage image, int maxBytes) {
        requireConfigured();
        return guarded("the upload could not be read", () -> client.readHeader(publicId(image), maxBytes));
    }

    @Override
    public void delete(OwnedImage image) {
        requireConfigured();
        guarded("the image could not be deleted", () -> {
            client.destroy(publicId(image));
            return Boolean.TRUE;
        });
    }

    /** Cloudinary rejects other formats on upload but keeps whatever it accepted as is, so the service reads the header itself. */
    @Override
    public boolean validatesOnUpload() {
        return false;
    }

    private static String publicId(OwnedImage image) {
        return ImagePath.of(image);
    }

    private static String contentTypeOf(String format) {
        String name = format == null ? "" : format.strip().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "image/" + name;
        };
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

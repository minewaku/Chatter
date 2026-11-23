package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

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
 * The adapter for {@link ImageStorage} on Cloudinary. Clients upload straight to Cloudinary with a signed form POST whose
 * {@code public_id} is the path {@link ImagePath} names and which may create that asset only, never overwrite one;
 * nothing is ever copied or renamed. Cloudinary cannot pin a file's length or content type in the signature, so a
 * ticket does not hold the upload to the length it was issued for (only the account's own upload limit does); the
 * service reads the file's own leading bytes after the upload through {@link #readLeadingBytes}, which is why
 * {@link #validatesOnUpload()} stays false. Everything Cloudinary-shaped is decided here and the SDK stays behind
 * {@link CloudinaryClient}.
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

    /** {@code contentType} and {@code sizeBytes} cannot be part of a Cloudinary signature, so they are not enforced on upload. */
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

    /** The asset lookup says whether the upload arrived; the bytes come from a ranged read. Not there is empty. */
    @Override
    public Optional<byte[]> readLeadingBytes(OwnedImage image, int maxBytes) {
        requireConfigured();
        return guarded("the upload could not be read", () -> client.asset(publicId(image))
                .map(asset -> client.readLeadingBytes(publicId(image), maxBytes)));
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

package com.minewaku.chatter.profilenext.start.support;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.model.ConfirmedUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImageInfo;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImagePage;
import com.minewaku.chatter.profilenext.profile.internal.model.UploadPermission;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A stand-in for the provider that signs and verifies deterministically, keeps the "stored files" in memory and can be
 * switched to failing. A notification body is {@code userId;slot;uploadId;format;bytes}; its signature is
 * {@code sig:} plus the body.
 */
public class FakeCloudStorage implements ImageStorage {

    public final Map<String, Instant> files = new ConcurrentHashMap<>();
    public volatile boolean failing;

    public static String publicId(long userId, ImageSlot slot, String uploadId) {
        return "chatter/profile/" + slot.folder() + "/" + userId + "/" + uploadId;
    }

    public static String body(long userId, ImageSlot slot, String uploadId, String format, long bytes) {
        return userId + ";" + slot.name() + ";" + uploadId + ";" + format + ";" + bytes;
    }

    public static String signatureFor(String body) {
        return "sig:" + body;
    }

    public static String urlOf(String publicId) {
        return "https://img.example/" + publicId;
    }

    /** What the client's direct upload to the provider would do. */
    public void upload(long userId, ImageSlot slot, String uploadId) {
        files.put(publicId(userId, slot, uploadId), Instant.now());
    }

    @Override
    public UploadPermission permit(UserId userId, ImageSlot slot, String uploadId, Instant now) {
        return new UploadPermission(
                "https://upload.example/image",
                "POST",
                Map.of("public_id", publicId(userId.value(), slot, uploadId)),
                now.plusSeconds(900));
    }

    @Override
    public Optional<ConfirmedUpload> verify(byte[] rawBody, String signature, String timestamp, Instant now) {
        String body = new String(rawBody, StandardCharsets.UTF_8);
        if (!signatureFor(body).equals(signature)) {
            return Optional.empty();
        }
        String[] parts = body.split(";");
        long userId = Long.parseLong(parts[0]);
        ImageSlot slot = ImageSlot.valueOf(parts[1]);
        String publicId = publicId(userId, slot, parts[2]);
        return Optional.of(new ConfirmedUpload(
                UserId.of(userId), slot, publicId, urlOf(publicId), parts[3], Long.parseLong(parts[4])));
    }

    @Override
    public void remove(String publicId) {
        if (failing) {
            throw new ImageStorageUnavailableException("provider unavailable");
        }
        files.remove(publicId);
    }

    @Override
    public StoredImagePage list(String cursor) {
        if (failing) {
            throw new ImageStorageUnavailableException("provider unavailable");
        }
        List<StoredImageInfo> items = files.entrySet().stream()
                .map(entry -> new StoredImageInfo(entry.getKey(), entry.getValue()))
                .toList();
        return new StoredImagePage(items, null);
    }
}

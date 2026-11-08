package com.minewaku.chatter.profilenext.profile.internal.port;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageDimensions;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The image storage provider, seen from core. No method exposes a provider type, a path or an address: the core names an
 * image by owner, slot and id, and the adapter alone knows where that lives.
 */
public interface ImageStorage {

    /**
     * Signs an upload that can only go to the temporary location of exactly this owner, slot and id, and can be used
     * once. Throws on provider trouble.
     */
    UploadPermission permit(OwnedImage image, Instant now);

    /**
     * Checks a provider notification (signature over the raw body, freshness, shape, and that the location is a
     * temporary-upload location of a known slot with a valid user and image id). Empty when any check fails, never
     * throws for bad input. Owner, slot and id come only from the signed location.
     */
    Optional<ConfirmedUpload> verify(byte[] rawBody, String signature, String timestamp, Instant now);

    /**
     * Moves the image from temporary to permanent storage without overwriting. A permanent copy that already exists
     * counts as success, so a repeat after a crash completes. Any other failure throws.
     */
    void promote(OwnedImage image);

    /** Deletes the permanent and the temporary copy. A copy that is already gone counts as success; other failures throw. */
    void remove(OwnedImage image);

    /** One page of the <em>permanent</em> images this service keeps (never temporary ones); order is not guaranteed. */
    StoredImagePage list(String cursor);

    /** What a client needs to upload one file straight to the provider: send {@code fields} plus the file to the URL. */
    record UploadPermission(String uploadUrl, String method, Map<String, String> fields, Instant expiresAt) {}

    /** A provider notification that passed its checks: who it is for, which slot and id, and what was stored. */
    record ConfirmedUpload(
            UserId userId, ImageSlot slot, ImageId imageId, String format, long bytes, ImageDimensions dimensions) {

        public ConfirmedUpload {
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(slot, "slot");
            Objects.requireNonNull(imageId, "imageId");
            Objects.requireNonNull(dimensions, "dimensions");
        }

        public OwnedImage image() {
            return new OwnedImage(userId, slot, imageId);
        }
    }

    /** A permanent image the provider holds and when it was created. */
    record StoredImageInfo(OwnedImage image, Instant createdAt) {}

    /** One page of the provider's listing; {@code nextCursor} is null on the last page. */
    record StoredImagePage(List<StoredImageInfo> items, String nextCursor) {}
}

package com.minewaku.chatter.profilenext.profile.internal.port;

import com.minewaku.chatter.profilenext.profile.internal.model.ConfirmedUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImagePage;
import com.minewaku.chatter.profilenext.profile.internal.model.UploadPermission;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Instant;
import java.util.Optional;

/** The image storage provider, seen from core. No method exposes a provider type. */
public interface ImageStorage {

    /** Signs an upload that may only go to this user's own location for this slot. */
    UploadPermission permit(UserId userId, ImageSlot slot, String uploadId, Instant now);

    /**
     * Checks a provider notification (signature over the raw body, freshness, shape). Empty when any check fails;
     * otherwise the values from the signed notification.
     */
    Optional<ConfirmedUpload> verify(byte[] rawBody, String signature, String timestamp, Instant now);

    /** Deletes a file. A file that is already gone counts as success; any other failure throws. */
    void remove(String publicId);

    /** One page of the files this service keeps, oldest-first order not guaranteed. */
    StoredImagePage list(String cursor);
}

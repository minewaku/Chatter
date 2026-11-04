package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageRateLimitedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.model.ConfirmedUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImageInfo;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImagePage;
import com.minewaku.chatter.profilenext.profile.internal.model.UploadPermission;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A programmable stand-in for the provider. */
public class FakeImageStorage implements ImageStorage {

    public record Permit(UserId userId, ImageSlot slot, String uploadId, Instant now) {}

    public final List<Permit> permits = new ArrayList<>();
    public final List<String> removed = new ArrayList<>();
    public final List<String> removeCalls = new ArrayList<>();
    public final List<StoredImagePage> pages = new ArrayList<>();
    public final List<String> listCursors = new ArrayList<>();

    /** What {@link #verify} answers; empty means "bad signature". */
    public Optional<ConfirmedUpload> verifyResult = Optional.empty();

    public boolean failRemoval;
    public int listsBeforeRateLimit = Integer.MAX_VALUE;

    @Override
    public UploadPermission permit(UserId userId, ImageSlot slot, String uploadId, Instant now) {
        permits.add(new Permit(userId, slot, uploadId, now));
        String publicId = "chatter/profile/" + slot.folder() + "/" + userId.value() + "/" + uploadId;
        return new UploadPermission(
                "https://upload.example/image", "POST", Map.of("public_id", publicId), now.plusSeconds(900));
    }

    @Override
    public Optional<ConfirmedUpload> verify(byte[] rawBody, String signature, String timestamp, Instant now) {
        return verifyResult;
    }

    @Override
    public void remove(String publicId) {
        removeCalls.add(publicId);
        if (failRemoval) {
            throw new ImageStorageUnavailableException("provider down");
        }
        removed.add(publicId);
    }

    @Override
    public StoredImagePage list(String cursor) {
        listCursors.add(cursor);
        if (listCursors.size() > listsBeforeRateLimit) {
            throw new ImageStorageRateLimitedException("slow down");
        }
        int index = cursor == null ? 0 : Integer.parseInt(cursor);
        return pages.get(index);
    }

    public static StoredImagePage page(String nextCursor, StoredImageInfo... items) {
        return new StoredImagePage(List.of(items), nextCursor);
    }
}

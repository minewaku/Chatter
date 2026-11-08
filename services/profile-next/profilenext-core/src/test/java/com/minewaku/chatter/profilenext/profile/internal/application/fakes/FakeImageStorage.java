package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.ConfirmedUpload;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.StoredImageInfo;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.StoredImagePage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.UploadPermission;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageRateLimitedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** A programmable stand-in for the provider. It lists only what a test gives it, as the real port lists permanent images only. */
public class FakeImageStorage implements ImageStorage {

    public record Permit(OwnedImage image, Instant now) {}

    public final List<Permit> permits = new ArrayList<>();
    public final List<OwnedImage> promoteCalls = new ArrayList<>();
    public final List<OwnedImage> removeCalls = new ArrayList<>();
    public final List<OwnedImage> removed = new ArrayList<>();
    public final List<StoredImagePage> pages = new ArrayList<>();
    public final List<String> listCursors = new ArrayList<>();

    /** What {@link #verify} answers; empty means "not accepted". */
    public Optional<ConfirmedUpload> verifyResult = Optional.empty();

    /** Images whose promotion fails with an outage. */
    public final Set<ImageId> failingPromotions = new HashSet<>();

    /** Images whose removal fails with an outage (in addition to {@link #failRemoval}). */
    public final Set<ImageId> failingRemovals = new HashSet<>();

    public boolean failRemoval;
    public boolean failPermit;
    public boolean rateLimitPermit;
    public int listsBeforeRateLimit = Integer.MAX_VALUE;

    @Override
    public UploadPermission permit(OwnedImage image, Instant now) {
        if (rateLimitPermit) {
            throw new ImageStorageRateLimitedException("slow down");
        }
        if (failPermit) {
            throw new ImageStorageUnavailableException("provider down");
        }
        permits.add(new Permit(image, now));
        return new UploadPermission(
                "https://upload.example/image",
                "POST",
                Map.of(
                        "user_id", Long.toString(image.userId().value()),
                        "slot", image.slot().name(),
                        "image_id", image.imageId().value().toString()),
                now.plusSeconds(900));
    }

    @Override
    public Optional<ConfirmedUpload> verify(byte[] rawBody, String signature, String timestamp, Instant now) {
        return verifyResult;
    }

    @Override
    public void promote(OwnedImage image) {
        promoteCalls.add(image);
        if (failingPromotions.contains(image.imageId())) {
            throw new ImageStorageUnavailableException("provider down");
        }
    }

    @Override
    public void remove(OwnedImage image) {
        removeCalls.add(image);
        if (failRemoval || failingRemovals.contains(image.imageId())) {
            throw new ImageStorageUnavailableException("provider down");
        }
        removed.add(image);
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

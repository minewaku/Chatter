package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingUploadRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** The upload ledger in memory, with the same rules as the SQL adapter. */
public class FakePendingUploads implements PendingUploadRepository {

    public final Map<ImageId, PendingUpload> rows = new LinkedHashMap<>();

    @Override
    public Optional<PendingUpload> findPending(UserId userId, ImageId imageId) {
        return Optional.ofNullable(rows.get(imageId))
                .filter(PendingUpload::isPending)
                .filter(u -> u.image().userId().equals(userId));
    }

    @Override
    public Optional<PendingUpload> lockPending(UserId userId, ImageId imageId) {
        return findPending(userId, imageId);
    }

    @Override
    public void insert(PendingUpload upload) {
        rows.put(upload.image().imageId(), upload);
    }

    @Override
    public void discard(OwnedImage image, Instant deleteAfter) {
        rows.put(image.imageId(), PendingUpload.discard(image, deleteAfter));
    }

    @Override
    public void discardAllPending(UserId userId) {
        for (PendingUpload upload : java.util.List.copyOf(rows.values())) {
            if (upload.isPending() && upload.image().userId().equals(userId)) {
                rows.put(upload.image().imageId(), PendingUpload.discard(upload.image(), upload.deleteAfter()));
            }
        }
    }

    @Override
    public void discardPending(UserId userId, ImageSlot slot) {
        for (PendingUpload upload : java.util.List.copyOf(rows.values())) {
            if (upload.isPending() && upload.image().userId().equals(userId) && upload.image().slot() == slot) {
                rows.put(upload.image().imageId(), PendingUpload.discard(upload.image(), upload.deleteAfter()));
            }
        }
    }

    @Override
    public void delete(ImageId imageId) {
        rows.remove(imageId);
    }

    @Override
    public Optional<PendingUpload> lockNextDue(Instant now) {
        return rows.values().stream()
                .filter(u -> u.isDueAt(now))
                .min(Comparator.comparing(PendingUpload::deleteAfter));
    }

    @Override
    public void postpone(ImageId imageId, Instant deleteAfter) {
        PendingUpload row = rows.get(imageId);
        rows.put(imageId, new PendingUpload(row.image(), row.expiresAt(), deleteAfter));
    }
}

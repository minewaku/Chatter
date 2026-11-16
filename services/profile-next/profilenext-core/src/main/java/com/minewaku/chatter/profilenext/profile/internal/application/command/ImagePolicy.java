package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.StoredObject;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The rules an uploaded image must meet and how long an upload ticket lives. Pure: every check returns the reason it
 * failed, or empty when the input passes. The same limits are checked three times on purpose: on the declared values when
 * the ticket is requested, on what the storage really holds, and on what the file's own bytes say.
 *
 * @param allowedFormats short names ({@code jpg}, {@code png}, {@code webp})
 * @param ticketLifetime how long an upload ticket works
 * @param grace how long after a ticket expires its key may still be swept, so a slow upload is never deleted under way
 * @param headerBytes how much of the stored file is read to tell what it is
 */
public record ImagePolicy(
        long maxBytesAvatar,
        long maxBytesBanner,
        int maxDimensionAvatar,
        int maxDimensionBanner,
        Set<String> allowedFormats,
        Duration ticketLifetime,
        Duration grace,
        int headerBytes) {

    public long maxBytes(ImageSlot slot) {
        return slot == ImageSlot.AVATAR ? maxBytesAvatar : maxBytesBanner;
    }

    public int maxDimension(ImageSlot slot) {
        return slot == ImageSlot.AVATAR ? maxDimensionAvatar : maxDimensionBanner;
    }

    /** The short format name for a content type ({@code image/jpeg} gives {@code jpg}), or empty when it is not one we know. */
    public static Optional<String> formatOf(String contentType) {
        if (contentType == null) {
            return Optional.empty();
        }
        return switch (contentType.strip().toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> Optional.of("jpg");
            case "image/png" -> Optional.of("png");
            case "image/webp" -> Optional.of("webp");
            default -> Optional.empty();
        };
    }

    /** The values the client declared when asking for a ticket. */
    public Optional<Reason> checkRequest(ImageSlot slot, String contentType, long sizeBytes) {
        if (formatOf(contentType).filter(allowedFormats::contains).isEmpty()) {
            return Optional.of(Reason.FORMAT_NOT_ALLOWED);
        }
        if (sizeBytes <= 0 || sizeBytes > maxBytes(slot)) {
            return Optional.of(Reason.FILE_TOO_LARGE);
        }
        return Optional.empty();
    }

    /** What the storage reports about the arrived file. */
    public Optional<Reason> checkStored(ImageSlot slot, StoredObject stored) {
        if (formatOf(stored.contentType()).filter(allowedFormats::contains).isEmpty()) {
            return Optional.of(Reason.FORMAT_NOT_ALLOWED);
        }
        if (stored.sizeBytes() <= 0 || stored.sizeBytes() > maxBytes(slot)) {
            return Optional.of(Reason.FILE_TOO_LARGE);
        }
        return Optional.empty();
    }

    /** What the file's own first bytes say, against what the storage recorded for it. */
    public Optional<Reason> checkContent(ImageSlot slot, StoredObject stored, SniffedImage sniffed) {
        if (!allowedFormats.contains(sniffed.format())) {
            return Optional.of(Reason.FORMAT_NOT_ALLOWED);
        }
        if (!formatOf(stored.contentType()).filter(sniffed.format()::equals).isPresent()) {
            return Optional.of(Reason.CONTENT_TYPE_MISMATCH);
        }
        int limit = maxDimension(slot);
        if (sniffed.dimensions().width() > limit || sniffed.dimensions().height() > limit) {
            return Optional.of(Reason.DIMENSIONS_NOT_ALLOWED);
        }
        return Optional.empty();
    }
}

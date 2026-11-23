package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The rules an uploaded image must meet and how long an upload ticket lives. Pure: every check returns the reason it
 * failed, or empty when the input passes. The two checks answer different questions: {@link #checkRequest} judges what
 * the client <em>declares</em> when it asks for a ticket (type and length), {@link #checkContent} judges what the
 * stored file <em>is</em> (format and dimensions, read from its own bytes). The real length is not checked here: it is
 * the storage's job to hold the uploaded file to the length the ticket was issued for.
 *
 * @param allowedFormats short names ({@code jpg}, {@code png}, {@code webp})
 * @param ticketLifetime how long an upload ticket works
 * @param grace how long after a ticket expires its key may still be swept, so a slow upload is never deleted under way
 * @param inspectBytes how many first bytes of the stored file are read to tell what it is
 */
public record ImagePolicy(
        long maxBytesAvatar,
        long maxBytesBanner,
        int maxDimensionAvatar,
        int maxDimensionBanner,
        Set<String> allowedFormats,
        Duration ticketLifetime,
        Duration grace,
        int inspectBytes) {

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

    /** The values the client declared when asking for a ticket; nothing here is known to be true yet. */
    public Optional<Reason> checkRequest(ImageSlot slot, String contentType, long sizeBytes) {
        if (formatOf(contentType).filter(allowedFormats::contains).isEmpty()) {
            return Optional.of(Reason.FORMAT_NOT_ALLOWED);
        }
        if (sizeBytes <= 0 || sizeBytes > maxBytes(slot)) {
            return Optional.of(Reason.FILE_TOO_LARGE);
        }
        return Optional.empty();
    }

    /** What the file's own first bytes say it is. */
    public Optional<Reason> checkContent(ImageSlot slot, SniffedImage sniffed) {
        if (!allowedFormats.contains(sniffed.format())) {
            return Optional.of(Reason.FORMAT_NOT_ALLOWED);
        }
        int limit = maxDimension(slot);
        if (sniffed.dimensions().width() > limit || sniffed.dimensions().height() > limit) {
            return Optional.of(Reason.DIMENSIONS_NOT_ALLOWED);
        }
        return Optional.empty();
    }
}

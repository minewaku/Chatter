package com.minewaku.chatter.profile.profile.internal.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * One row of the upload ledger: a storage key the service authorized or retired and that is not (or no longer) the
 * image of a profile. Every key that can exist in storage has a row until it is deleted or becomes current.
 *
 * <ul>
 *   <li>pending ({@code expiresAt} set): a ticket was issued and the upload may still arrive. {@code expiresAt} is the
 *       last moment the ticket works; {@code deleteAfter} is {@code expiresAt + grace}.
 *   <li>discarded ({@code expiresAt} null): the key must be deleted once {@code deleteAfter} has passed.
 * </ul>
 */
public record PendingUpload(OwnedImage image, Instant expiresAt, Instant deleteAfter) {

    public PendingUpload {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(deleteAfter, "deleteAfter");
    }

    /** A new ticket: usable for {@code ticketLifetime}, its key swept {@code grace} after that. */
    public static PendingUpload issue(OwnedImage image, Instant now, Duration ticketLifetime, Duration grace) {
        Instant expiresAt = now.plus(ticketLifetime);
        return new PendingUpload(image, expiresAt, expiresAt.plus(grace));
    }

    /** A key to delete once {@code deleteAfter} has passed. */
    public static PendingUpload discard(OwnedImage image, Instant deleteAfter) {
        return new PendingUpload(image, null, deleteAfter);
    }

    public boolean isPending() {
        return expiresAt != null;
    }

    /** True once the ticket can no longer be committed. Only a pending upload expires. */
    public boolean isExpiredAt(Instant now) {
        return isPending() && !now.isBefore(expiresAt);
    }

    /** True when the sweeper may delete the key. */
    public boolean isDueAt(Instant now) {
        return !now.isBefore(deleteAfter);
    }
}

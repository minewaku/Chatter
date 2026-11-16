package com.minewaku.chatter.profilenext.profile.internal.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * One row of the upload ledger: a storage key the service authorized or retired and that is not (or no longer) the
 * image of a profile. Every key that can exist in storage has a row until it is deleted or becomes current.
 *
 * <ul>
 *   <li>{@link State#PENDING}: a ticket was issued and the upload may still arrive. {@code expiresAt} is the last moment
 *       the ticket works; {@code deleteAfter} is {@code expiresAt + grace}.
 *   <li>{@link State#DISCARD}: the key must be deleted once {@code deleteAfter} has passed.
 * </ul>
 */
public record PendingUpload(OwnedImage image, State state, Instant expiresAt, Instant deleteAfter) {

    public enum State {
        PENDING,
        DISCARD
    }

    /** A pending ticket with less time left than this is replaced instead of handed out again. */
    public static final Duration MIN_REUSE_WINDOW = Duration.ofSeconds(30);

    public PendingUpload {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(deleteAfter, "deleteAfter");
        if (state == State.PENDING && expiresAt == null) {
            throw new IllegalArgumentException("a pending upload needs an expiry");
        }
    }

    /** A new ticket: usable for {@code ticketLifetime}, its key swept {@code grace} after that. */
    public static PendingUpload issue(OwnedImage image, Instant now, Duration ticketLifetime, Duration grace) {
        Instant expiresAt = now.plus(ticketLifetime);
        return new PendingUpload(image, State.PENDING, expiresAt, expiresAt.plus(grace));
    }

    /** A key to delete once {@code deleteAfter} has passed. */
    public static PendingUpload discard(OwnedImage image, Instant deleteAfter) {
        return new PendingUpload(image, State.DISCARD, null, deleteAfter);
    }

    public boolean isPending() {
        return state == State.PENDING;
    }

    /** True once the ticket can no longer be committed. Only a pending upload expires. */
    public boolean isExpiredAt(Instant now) {
        return isPending() && !now.isBefore(expiresAt);
    }

    /** True when the same ticket can be handed out again: pending, with at least {@link #MIN_REUSE_WINDOW} left. */
    public boolean isReusableAt(Instant now) {
        return isPending() && Duration.between(now, expiresAt).compareTo(MIN_REUSE_WINDOW) >= 0;
    }

    /** True when the sweeper may delete the key. */
    public boolean isDueAt(Instant now) {
        return !now.isBefore(deleteAfter);
    }
}

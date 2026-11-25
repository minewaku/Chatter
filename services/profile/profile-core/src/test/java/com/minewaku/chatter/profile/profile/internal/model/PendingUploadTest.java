package com.minewaku.chatter.profile.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profile.profile.internal.application.fakes.ImageIds;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PendingUploadTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final OwnedImage IMAGE = new OwnedImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(1));

    private final PendingUpload ticket = PendingUpload.issue(IMAGE, NOW, Duration.ofMinutes(5), Duration.ofMinutes(10));

    @Test
    void aTicketExpiresAfterItsLifetimeAndIsSweptAfterTheGraceOnTop() {
        assertThat(ticket.isPending()).isTrue();
        assertThat(ticket.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(ticket.deleteAfter()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void itIsExpiredExactlyAtItsExpiryNotBefore() {
        assertThat(ticket.isExpiredAt(NOW.plus(Duration.ofMinutes(5)).minusMillis(1))).isFalse();
        assertThat(ticket.isExpiredAt(NOW.plus(Duration.ofMinutes(5)))).isTrue();
    }

    @Test
    void aDiscardedKeyNeverExpiresAndIsDueAtItsSweepTime() {
        PendingUpload discarded = PendingUpload.discard(IMAGE, NOW);

        assertThat(discarded.isPending()).isFalse();
        assertThat(discarded.isExpiredAt(NOW.plus(Duration.ofDays(1)))).isFalse();
        assertThat(discarded.isDueAt(NOW.minusMillis(1))).isFalse();
        assertThat(discarded.isDueAt(NOW)).isTrue();
    }

    @Test
    void aDeleteTimeIsRequired() {
        assertThatThrownBy(() -> new PendingUpload(IMAGE, null, null)).isInstanceOf(NullPointerException.class);
    }
}

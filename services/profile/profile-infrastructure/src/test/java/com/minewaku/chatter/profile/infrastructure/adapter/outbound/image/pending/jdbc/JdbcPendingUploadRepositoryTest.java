package com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.pending.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profile.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.profile.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.profile.profile.internal.model.ImageId;
import com.minewaku.chatter.profile.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profile.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profile.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import com.minewaku.chatter.profile.profile.internal.port.repository.PendingUploadRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(JdbcTestConfiguration.class)
class JdbcPendingUploadRepositoryTest extends PostgreSqlTestSupport {

    private static final AtomicLong USERS = new AtomicLong(7_000_000L);
    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    @Autowired
    private PendingUploadRepository ledger;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TransactionTemplate transaction;

    @BeforeEach
    void emptyLedger() {
        jdbc.sql("DELETE FROM pending_upload").update(); // lockNextDue looks at the whole table
    }

    private static UserId freshUser() {
        return UserId.of(USERS.incrementAndGet());
    }

    private static OwnedImage image(UserId user, ImageSlot slot) {
        return new OwnedImage(user, slot, new ImageId(UUID.randomUUID()));
    }

    private static PendingUpload ticket(OwnedImage image, Instant now) {
        return PendingUpload.issue(image, now, Duration.ofMinutes(5), Duration.ofMinutes(10));
    }

    private int rows(UserId user) {
        return jdbc.sql("SELECT count(*) FROM pending_upload WHERE user_id = :id")
                .param("id", user.value())
                .query(Integer.class)
                .single();
    }

    @Test
    void aTicketRoundTripsWithAllItsTimes() {
        UserId user = freshUser();
        PendingUpload ticket = ticket(image(user, ImageSlot.AVATAR), NOW);

        ledger.insert(ticket);

        assertThat(ledger.findPending(user, ticket.image().imageId())).contains(ticket);
    }

    @Test
    void aUserMayHoldManyPendingTicketsForOneSlot() {
        UserId user = freshUser();
        PendingUpload first = ticket(image(user, ImageSlot.AVATAR), NOW);
        PendingUpload second = ticket(image(user, ImageSlot.AVATAR), NOW);

        ledger.insert(first);
        ledger.insert(second);

        assertThat(ledger.findPending(user, first.image().imageId())).contains(first);
        assertThat(ledger.findPending(user, second.image().imageId())).contains(second);
        assertThat(rows(user)).isEqualTo(2);
    }

    @Test
    void findingByImageIdIsScopedToTheOwnerAndToPendingTickets() {
        UserId user = freshUser();
        PendingUpload ticket = ticket(image(user, ImageSlot.AVATAR), NOW);
        ledger.insert(ticket);

        assertThat(ledger.findPending(freshUser(), ticket.image().imageId())).isEmpty();

        ledger.discard(ticket.image(), NOW);
        assertThat(ledger.findPending(user, ticket.image().imageId())).isEmpty();
    }

    @Test
    void discardingATicketKeepsItsRowAndTurnsItIntoADiscard() {
        UserId user = freshUser();
        PendingUpload old = ticket(image(user, ImageSlot.AVATAR), NOW);
        ledger.insert(old);

        ledger.discard(old.image(), old.deleteAfter());

        assertThat(rows(user)).isEqualTo(1);
        assertThat(isPending(old.image().imageId())).isFalse();
        assertThat(deleteAfter(old.image().imageId())).isEqualTo(old.deleteAfter());
    }

    @Test
    void discardingAnImageWithNoRowAddsOne() {
        UserId user = freshUser();
        OwnedImage replaced = image(user, ImageSlot.BANNER);

        ledger.discard(replaced, NOW);

        assertThat(isPending(replaced.imageId())).isFalse();
        assertThat(deleteAfter(replaced.imageId())).isEqualTo(NOW);
    }

    @Test
    void discardAllPendingRetiresEveryTicketOfTheUserKeepingTheirSweepTimes() {
        UserId user = freshUser();
        UserId other = freshUser();
        PendingUpload avatar = ticket(image(user, ImageSlot.AVATAR), NOW);
        PendingUpload banner = ticket(image(user, ImageSlot.BANNER), NOW);
        PendingUpload untouched = ticket(image(other, ImageSlot.AVATAR), NOW);
        ledger.insert(avatar);
        ledger.insert(banner);
        ledger.insert(untouched);

        ledger.discardAllPending(user);

        assertThat(ledger.findPending(user, avatar.image().imageId())).isEmpty();
        assertThat(ledger.findPending(user, banner.image().imageId())).isEmpty();
        assertThat(ledger.findPending(other, untouched.image().imageId())).contains(untouched);
        assertThat(isPending(avatar.image().imageId())).isFalse();
        assertThat(deleteAfter(avatar.image().imageId())).isEqualTo(avatar.deleteAfter());
    }

    @Test
    void discardPendingRetiresOnlyTheTicketsOfThatUserAndSlotKeepingTheirSweepTimes() {
        UserId user = freshUser();
        UserId other = freshUser();
        PendingUpload first = ticket(image(user, ImageSlot.AVATAR), NOW);
        PendingUpload second = ticket(image(user, ImageSlot.AVATAR), NOW);
        PendingUpload banner = ticket(image(user, ImageSlot.BANNER), NOW);
        PendingUpload others = ticket(image(other, ImageSlot.AVATAR), NOW);
        List.of(first, second, banner, others).forEach(ledger::insert);

        ledger.discardPending(user, ImageSlot.AVATAR);

        assertThat(isPending(first.image().imageId())).isFalse();
        assertThat(isPending(second.image().imageId())).isFalse();
        assertThat(deleteAfter(first.image().imageId())).isEqualTo(first.deleteAfter());
        assertThat(ledger.findPending(user, banner.image().imageId())).contains(banner);
        assertThat(ledger.findPending(other, others.image().imageId())).contains(others);
    }

    @Test
    void deleteRemovesTheRowAndRepeatingIsHarmless() {
        UserId user = freshUser();
        PendingUpload ticket = ticket(image(user, ImageSlot.AVATAR), NOW);
        ledger.insert(ticket);

        ledger.delete(ticket.image().imageId());
        ledger.delete(ticket.image().imageId());

        assertThat(rows(user)).isZero();
    }

    @Test
    void lockNextDueReturnsTheOldestDueRowAndNothingBeforeItsTime() {
        UserId user = freshUser();
        OwnedImage soon = image(user, ImageSlot.AVATAR);
        OwnedImage later = image(user, ImageSlot.BANNER);
        ledger.discard(later, NOW.plusSeconds(60));
        ledger.discard(soon, NOW.plusSeconds(10));

        transaction.executeWithoutResult(status -> {
            assertThat(ledger.lockNextDue(NOW.plusSeconds(5))).isEmpty();
            assertThat(ledger.lockNextDue(NOW.plusSeconds(10)).map(PendingUpload::image)).contains(soon);
        });
    }

    @Test
    void aLockedRowIsSkippedByAnotherWorker() throws Exception {
        UserId user = freshUser();
        OwnedImage only = image(user, ImageSlot.AVATAR);
        ledger.discard(only, NOW);

        transaction.executeWithoutResult(status -> {
            assertThat(ledger.lockNextDue(NOW).map(PendingUpload::image)).contains(only);
            Optional<PendingUpload> seenByOther = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> transaction.execute(inner -> ledger.lockNextDue(NOW)))
                    .join();
            assertThat(seenByOther.map(PendingUpload::image)).isNotEqualTo(Optional.of(only));
        });
    }

    @Test
    void postponingMovesTheSweepTimeForward() {
        UserId user = freshUser();
        OwnedImage image = image(user, ImageSlot.AVATAR);
        ledger.discard(image, NOW);

        ledger.postpone(image.imageId(), NOW.plus(Duration.ofMinutes(5)));

        assertThat(deleteAfter(image.imageId())).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    private boolean isPending(ImageId id) {
        return jdbc.sql("SELECT expires_at IS NOT NULL FROM pending_upload WHERE image_id = :id")
                .param("id", id.value())
                .query(Boolean.class)
                .single();
    }

    private Instant deleteAfter(ImageId id) {
        return jdbc.sql("SELECT delete_after FROM pending_upload WHERE image_id = :id")
                .param("id", id.value())
                .query((rs, n) -> rs.getTimestamp(1).toInstant())
                .single();
    }
}

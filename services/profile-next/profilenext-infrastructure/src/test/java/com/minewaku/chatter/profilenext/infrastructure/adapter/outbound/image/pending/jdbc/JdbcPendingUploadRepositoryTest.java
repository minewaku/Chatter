package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.pending.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.profilenext.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingUploadRepository;
import java.time.Duration;
import java.time.Instant;
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

        assertThat(ledger.insertPending(ticket)).isTrue();

        assertThat(ledger.findPending(user, ImageSlot.AVATAR)).contains(ticket);
        assertThat(ledger.findPending(user, ticket.image().imageId())).contains(ticket);
    }

    @Test
    void aSecondPendingTicketForTheSameUserAndSlotIsNotInserted() {
        UserId user = freshUser();
        PendingUpload winner = ticket(image(user, ImageSlot.AVATAR), NOW);
        assertThat(ledger.insertPending(winner)).isTrue();

        assertThat(ledger.insertPending(ticket(image(user, ImageSlot.AVATAR), NOW))).isFalse();

        assertThat(ledger.findPending(user, ImageSlot.AVATAR)).contains(winner);
        assertThat(rows(user)).isEqualTo(1);
    }

    @Test
    void otherSlotsAndOtherUsersMayHoldTheirOwnTicket() {
        UserId user = freshUser();
        UserId other = freshUser();

        assertThat(ledger.insertPending(ticket(image(user, ImageSlot.AVATAR), NOW))).isTrue();
        assertThat(ledger.insertPending(ticket(image(user, ImageSlot.BANNER), NOW))).isTrue();
        assertThat(ledger.insertPending(ticket(image(other, ImageSlot.AVATAR), NOW))).isTrue();
    }

    @Test
    void findingByImageIdIsScopedToTheOwnerAndToPendingTickets() {
        UserId user = freshUser();
        PendingUpload ticket = ticket(image(user, ImageSlot.AVATAR), NOW);
        ledger.insertPending(ticket);

        assertThat(ledger.findPending(freshUser(), ticket.image().imageId())).isEmpty();

        ledger.discard(ticket.image(), NOW);
        assertThat(ledger.findPending(user, ticket.image().imageId())).isEmpty();
        assertThat(ledger.findPending(user, ImageSlot.AVATAR)).isEmpty();
    }

    @Test
    void discardingATicketKeepsItsRowTurnsItIntoADiscardAndFreesTheSlot() {
        UserId user = freshUser();
        PendingUpload old = ticket(image(user, ImageSlot.AVATAR), NOW);
        ledger.insertPending(old);

        ledger.discard(old.image(), old.deleteAfter());

        assertThat(rows(user)).isEqualTo(1);
        assertThat(ledger.insertPending(ticket(image(user, ImageSlot.AVATAR), NOW))).isTrue();
        assertThat(state(old.image().imageId())).isEqualTo("DISCARD");
    }

    @Test
    void discardingAnImageWithNoRowAddsOne() {
        UserId user = freshUser();
        OwnedImage replaced = image(user, ImageSlot.BANNER);

        ledger.discard(replaced, NOW);

        assertThat(state(replaced.imageId())).isEqualTo("DISCARD");
        assertThat(deleteAfter(replaced.imageId())).isEqualTo(NOW);
    }

    @Test
    void discardAllPendingRetiresEveryTicketOfTheUserKeepingTheirSweepTimes() {
        UserId user = freshUser();
        UserId other = freshUser();
        PendingUpload avatar = ticket(image(user, ImageSlot.AVATAR), NOW);
        PendingUpload banner = ticket(image(user, ImageSlot.BANNER), NOW);
        PendingUpload untouched = ticket(image(other, ImageSlot.AVATAR), NOW);
        ledger.insertPending(avatar);
        ledger.insertPending(banner);
        ledger.insertPending(untouched);

        ledger.discardAllPending(user);

        assertThat(ledger.findPending(user, ImageSlot.AVATAR)).isEmpty();
        assertThat(ledger.findPending(user, ImageSlot.BANNER)).isEmpty();
        assertThat(ledger.findPending(other, ImageSlot.AVATAR)).contains(untouched);
        assertThat(state(avatar.image().imageId())).isEqualTo("DISCARD");
        assertThat(deleteAfter(avatar.image().imageId())).isEqualTo(avatar.deleteAfter());
    }

    @Test
    void deleteRemovesTheRowAndRepeatingIsHarmless() {
        UserId user = freshUser();
        PendingUpload ticket = ticket(image(user, ImageSlot.AVATAR), NOW);
        ledger.insertPending(ticket);

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

    private String state(ImageId id) {
        return jdbc.sql("SELECT state FROM pending_upload WHERE image_id = :id")
                .param("id", id.value())
                .query(String.class)
                .single();
    }

    private Instant deleteAfter(ImageId id) {
        return jdbc.sql("SELECT delete_after FROM pending_upload WHERE image_id = :id")
                .param("id", id.value())
                .query((rs, n) -> rs.getTimestamp(1).toInstant())
                .single();
    }
}

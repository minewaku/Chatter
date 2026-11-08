package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** The fake encodes the port's contract; this keeps a wrong fake from making a service test pass falsely. */
class FakePromotionQueueTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final Duration LEASE = Duration.ofMinutes(5);

    private final FakePromotionQueue queue = new FakePromotionQueue();

    private static OwnedImage image(long user, int n) {
        return new OwnedImage(UserId.of(user), ImageSlot.AVATAR, ImageIds.at(n));
    }

    @Test
    void aDuplicateEnqueueIsIgnoredAndDoesNotResetAttempts() {
        queue.enqueue(image(7, 1), NOW);
        queue.markFailed(ImageIds.at(1), 3, NOW, null);

        queue.enqueue(image(7, 1), NOW.plusSeconds(60));

        assertThat(queue.rows).hasSize(1);
        assertThat(queue.rows.get(ImageIds.at(1)).attempts).isEqualTo(3);
    }

    @Test
    void aLeasedRowIsHiddenFromASecondCaller() {
        queue.enqueue(image(7, 1), NOW);

        assertThat(queue.takeDue(NOW, 10, LEASE)).hasSize(1);
        assertThat(queue.takeDue(NOW, 10, LEASE)).isEmpty();
        assertThat(queue.takeDue(NOW.plus(LEASE), 10, LEASE)).hasSize(1);
    }

    @Test
    void aReportedRowIsNeverTakenAgain() {
        queue.enqueue(image(7, 1), NOW);
        queue.markFailed(ImageIds.at(1), 9, NOW, NOW);

        assertThat(queue.takeDue(NOW.plus(Duration.ofDays(1)), 10, LEASE)).isEmpty();
    }

    @Test
    void removingAMissingIdIsANoOp() {
        queue.enqueue(image(7, 1), NOW);

        queue.remove(ImageIds.at(2));
        assertThat(queue.rows).hasSize(1);
        queue.remove(ImageIds.at(1));
        assertThat(queue.rows).isEmpty();
    }

    @Test
    void deleteByUserRemovesOnlyThatUsersRows() {
        queue.enqueue(image(7, 1), NOW);
        queue.enqueue(image(8, 2), NOW);

        queue.deleteByUserId(UserId.of(7));

        assertThat(queue.rows).containsOnlyKeys(ImageIds.at(2));
    }

    @Test
    void takeDueHonoursTheLimitAndReturnsTheCreatedTime() {
        queue.enqueue(image(7, 1), NOW);
        queue.enqueue(image(7, 2), NOW.plusSeconds(1));
        queue.enqueue(image(7, 3), NOW.plusSeconds(2));

        var taken = queue.takeDue(NOW.plusSeconds(10), 2, LEASE);

        assertThat(taken).hasSize(2);
        assertThat(taken.get(0).image().imageId()).isEqualTo(ImageIds.at(1));
        assertThat(taken.get(0).createdAt()).isEqualTo(NOW);
    }
}

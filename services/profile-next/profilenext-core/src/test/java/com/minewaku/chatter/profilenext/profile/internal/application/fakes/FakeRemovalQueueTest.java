package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The fake encodes the port's contract; this keeps a wrong fake from making a service test pass falsely. */
class FakeRemovalQueueTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final Duration LEASE = Duration.ofMinutes(5);

    private final FakeRemovalQueue queue = new FakeRemovalQueue();

    private static OwnedImage image(int n) {
        return new OwnedImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(n));
    }

    @Test
    void aDuplicateEnqueueIsIgnoredAndDoesNotResetAttempts() {
        queue.enqueue(image(1), NOW);
        queue.markFailed(ImageIds.at(1), 4, NOW, null);

        queue.enqueue(image(1), NOW.plusSeconds(30));

        assertThat(queue.rows).hasSize(1);
        assertThat(queue.rows.get(ImageIds.at(1)).attempts).isEqualTo(4);
    }

    @Test
    void aLeasedRowIsHiddenUntilTheLeaseEnds() {
        queue.enqueue(image(1), NOW);

        assertThat(queue.takeDue(NOW, 10, LEASE)).hasSize(1);
        assertThat(queue.takeDue(NOW, 10, LEASE)).isEmpty();
        assertThat(queue.takeDue(NOW.plus(LEASE), 10, LEASE)).hasSize(1);
    }

    @Test
    void aReportedRowIsNotTakenAndIsNotActive() {
        queue.enqueue(image(1), NOW);
        queue.enqueue(image(2), NOW);
        queue.markFailed(ImageIds.at(1), 11, NOW, NOW);

        assertThat(queue.takeDue(NOW.plus(Duration.ofDays(1)), 10, LEASE))
                .extracting(row -> row.image().imageId())
                .containsExactly(ImageIds.at(2));
        assertThat(queue.findActive(List.of(ImageIds.at(1), ImageIds.at(2)))).containsExactly(ImageIds.at(2));
    }

    @Test
    void markRemovedAndClearBothDeleteTheRow() {
        queue.enqueue(image(1), NOW);
        queue.enqueue(image(2), NOW);

        queue.markRemoved(ImageIds.at(1));
        queue.clear(ImageIds.at(2));

        assertThat(queue.rows).isEmpty();
    }
}

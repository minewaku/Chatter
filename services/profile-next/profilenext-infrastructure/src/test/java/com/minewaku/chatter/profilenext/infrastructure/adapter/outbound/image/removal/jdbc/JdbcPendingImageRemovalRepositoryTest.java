package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.removal.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.profilenext.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository.PendingRemoval;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(JdbcTestConfiguration.class)
class JdbcPendingImageRemovalRepositoryTest extends PostgreSqlTestSupport {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    @Autowired
    private PendingImageRemovalRepository queue;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager transactions;

    @BeforeEach
    void emptyQueue() {
        jdbc.sql("DELETE FROM pending_image_removal").update();
    }

    private static String id() {
        return "chatter/profile/avatars/1/" + UUID.randomUUID();
    }

    private long count() {
        return jdbc.sql("SELECT count(*) FROM pending_image_removal").query(Long.class).single();
    }

    @Test
    void enqueueingTwiceKeepsOneRow() {
        String id = id();

        queue.enqueue(id, ImageSlot.AVATAR, NOW);
        queue.enqueue(id, ImageSlot.AVATAR, NOW.plusSeconds(5));

        assertThat(count()).isEqualTo(1);
    }

    @Test
    void takeDueReturnsOnlyDueUnreportedRowsOldestFirstUpToTheLimit() {
        String first = id();
        String second = id();
        String later = id();
        String reported = id();
        queue.enqueue(first, ImageSlot.AVATAR, NOW.minusSeconds(30));
        queue.enqueue(second, ImageSlot.BANNER, NOW.minusSeconds(10));
        queue.enqueue(later, ImageSlot.AVATAR, NOW.plusSeconds(600));
        queue.enqueue(reported, ImageSlot.AVATAR, NOW.minusSeconds(60));
        queue.markFailed(reported, 11, NOW.minusSeconds(60), NOW.minusSeconds(60));

        List<PendingRemoval> taken = queue.takeDue(NOW, 10, Duration.ofMinutes(5));

        assertThat(taken).extracting(PendingRemoval::publicId).containsExactly(first, second);
        assertThat(taken.get(1).slot()).isEqualTo(ImageSlot.BANNER);

        queue.enqueue(id(), ImageSlot.AVATAR, NOW.minusSeconds(5));
        queue.enqueue(id(), ImageSlot.AVATAR, NOW.minusSeconds(4));
        assertThat(queue.takeDue(NOW, 1, Duration.ofMinutes(5))).hasSize(1);
    }

    @Test
    void aTakenRowIsNotReturnedAgainUntilItsLeaseHasPassed() {
        String id = id();
        queue.enqueue(id, ImageSlot.AVATAR, NOW);

        assertThat(queue.takeDue(NOW, 10, Duration.ofMinutes(5))).hasSize(1);
        assertThat(queue.takeDue(NOW.plusSeconds(60), 10, Duration.ofMinutes(5))).isEmpty();
        assertThat(queue.takeDue(NOW.plusSeconds(301), 10, Duration.ofMinutes(5)))
                .extracting(PendingRemoval::publicId)
                .containsExactly(id);
    }

    @Test
    void twoConcurrentTransactionsNeverReceiveTheSameRow() throws Exception {
        for (int i = 0; i < 4; i++) {
            queue.enqueue(id(), ImageSlot.AVATAR, NOW.minusSeconds(100 - i));
        }
        TransactionTemplate tx = new TransactionTemplate(transactions);
        CountDownLatch firstHolds = new CountDownLatch(1);
        CountDownLatch secondDone = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<List<PendingRemoval>> first = pool.submit(() -> tx.execute(status -> {
                List<PendingRemoval> taken = queue.takeDue(NOW, 2, Duration.ofMinutes(5));
                firstHolds.countDown();
                try {
                    secondDone.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return taken;
            }));
            firstHolds.await(10, TimeUnit.SECONDS);
            List<PendingRemoval> second = tx.execute(status -> queue.takeDue(NOW, 2, Duration.ofMinutes(5)));
            secondDone.countDown();

            assertThat(first.get()).hasSize(2);
            assertThat(second).hasSize(2);
            assertThat(first.get()).doesNotContainAnyElementsOf(second);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void markRemovedDeletesTheRow() {
        String id = id();
        queue.enqueue(id, ImageSlot.AVATAR, NOW);

        queue.markRemoved(id);

        assertThat(count()).isZero();
    }

    @Test
    void markFailedUpdatesAttemptsNextAttemptAndOptionallyReportedAt() {
        String id = id();
        queue.enqueue(id, ImageSlot.AVATAR, NOW);

        queue.markFailed(id, 3, NOW.plusSeconds(480), null);
        Map<String, Object> row = jdbc.sql("SELECT * FROM pending_image_removal WHERE public_id = :id")
                .param("id", id)
                .query()
                .singleRow();
        assertThat(row.get("attempts")).isEqualTo(3);
        assertThat(((java.sql.Timestamp) row.get("next_attempt_at")).toInstant()).isEqualTo(NOW.plusSeconds(480));
        assertThat(row.get("reported_at")).isNull();

        queue.markFailed(id, 11, NOW, NOW);
        Map<String, Object> reported = jdbc.sql("SELECT * FROM pending_image_removal WHERE public_id = :id")
                .param("id", id)
                .query()
                .singleRow();
        assertThat(((java.sql.Timestamp) reported.get("reported_at")).toInstant()).isEqualTo(NOW);
    }

    @Test
    void findActivePublicIdsIgnoresReportedRowsAndUnknownIds() {
        String active = id();
        String reported = id();
        queue.enqueue(active, ImageSlot.AVATAR, NOW);
        queue.enqueue(reported, ImageSlot.AVATAR, NOW);
        queue.markFailed(reported, 11, NOW, NOW);

        assertThat(queue.findActivePublicIds(List.of(active, reported, id()))).containsExactly(active);
        assertThat(queue.findActivePublicIds(List.of())).isEmpty();
    }

    @Test
    void clearDropsTheRow() {
        String id = id();
        queue.enqueue(id, ImageSlot.AVATAR, NOW);

        queue.clear(id);

        assertThat(count()).isZero();
    }
}

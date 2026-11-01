package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Closing an account through the API leaves exactly one announcement in the database change log (spec 013 US1). */
class AccountDeletionAnnouncementE2eTest extends E2eSupport {

    @Autowired
    private JdbcTemplate jdbc;

    private String slot;

    @BeforeEach
    void createSlot() {
        slot = "e2e_" + UUID.randomUUID().toString().replace('-', '_');
        jdbc.queryForList("SELECT pg_create_logical_replication_slot(?, 'test_decoding')", slot);
    }

    @AfterEach
    void dropSlot() {
        jdbc.queryForList("SELECT pg_drop_replication_slot(?)", slot);
    }

    private List<String> outboxChanges() {
        return jdbc.queryForList("SELECT data FROM pg_logical_slot_get_changes(?, NULL, NULL)", String.class, slot)
                .stream()
                .filter(line -> line.contains("identity_access.outbox"))
                .toList();
    }

    @Test
    void closingAnAccountWritesAndRemovesExactlyOneAnnouncementForThatUser() {
        String[] tokens = signIn(registerVerifiedAccount());
        String userId = claim(tokens[0], "sub");
        outboxChanges(); // registration and sign-in must not announce anything; start from a clean read

        ApiResponse closed = api().post("/users/@me/close", Map.of("currentPassword", PASSWORD), tokens[0]);

        assertThat(closed.status()).isEqualTo(204);
        List<String> changes = outboxChanges();
        assertThat(changes).hasSize(2);
        assertThat(changes.get(0)).contains("INSERT:").contains("aggregate_id[text]:'" + userId + "'");
        assertThat(changes.get(1)).contains("DELETE:");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM identity_access.outbox", Long.class))
                .isZero();
    }

    @Test
    void registrationAndSigningInAnnounceNothing() {
        outboxChanges();

        signIn(registerVerifiedAccount());

        assertThat(outboxChanges()).isEmpty();
    }

    @Test
    void twoCloseRequestsAtTheSameMomentAnnounceTheAccountOnlyOnce() throws Exception {
        String[] tokens = signIn(registerVerifiedAccount());
        String userId = claim(tokens[0], "sub");
        outboxChanges();
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<ApiResponse>> responses = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                responses.add(pool.submit(() -> {
                    go.await();
                    return api().post("/users/@me/close", Map.of("currentPassword", PASSWORD), tokens[0]);
                }));
            }
            go.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<ApiResponse> response : responses) {
                statuses.add(response.get().status());
            }

            assertThat(statuses).contains(204);
        } finally {
            pool.shutdownNow();
        }

        long inserts = outboxChanges().stream()
                .filter(line -> line.contains("INSERT:") && line.contains("aggregate_id[text]:'" + userId + "'"))
                .count();
        assertThat(inserts).isEqualTo(1);
    }
}

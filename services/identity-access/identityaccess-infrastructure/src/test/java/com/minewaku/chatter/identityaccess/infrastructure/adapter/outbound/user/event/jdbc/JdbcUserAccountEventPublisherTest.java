package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.event.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.support.ChangeLogProbe;
import com.minewaku.chatter.identityaccess.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.identityaccess.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.UserAccountEventPublisher;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * What the relay depends on is what lands in the database change log, so this reads the change log through a
 * {@code test_decoding} slot instead of mocking the repository.
 */
@SpringBootTest(classes = JdbcTestConfiguration.class)
class JdbcUserAccountEventPublisherTest extends PostgreSqlTestSupport {

    private static final Pattern PAYLOAD = Pattern.compile("payload\\[jsonb]:'(.*?)' occurred_at");
    private static final Instant DELETED_AT = Instant.parse("2026-10-29T08:15:30Z");

    @Autowired
    private UserAccountEventPublisher publisher;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    private ChangeLogProbe probe;

    @BeforeEach
    void startProbe() {
        probe = ChangeLogProbe.start(dataSource);
    }

    @AfterEach
    void stopProbe() {
        probe.close();
    }

    @Test
    void a_committed_call_leaves_one_insert_and_one_delete_in_the_change_log_and_an_empty_table() {
        transaction.executeWithoutResult(status -> publisher.publishDeleted(new UserId(123456789L), DELETED_AT));

        List<String> changes = probe.drainOutbox();

        assertThat(changes).hasSize(2);
        assertThat(changes.get(0)).contains("INSERT:");
        assertThat(changes.get(1)).contains("DELETE:");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox", Long.class)).isZero();
    }

    @Test
    void the_insert_carries_the_announcement_columns_and_a_payload_with_exactly_three_fields() throws Exception {
        transaction.executeWithoutResult(status -> publisher.publishDeleted(new UserId(123456789L), DELETED_AT));

        String insert = probe.drainOutbox().get(0);

        assertThat(insert)
                .contains("aggregate_type[text]:'user'")
                .contains("aggregate_id[text]:'123456789'")
                .contains("event_type[text]:'UserSoftDeleted'");
        Matcher matcher = PAYLOAD.matcher(insert);
        assertThat(matcher.find()).isTrue();
        JsonNode payload = objectMapper.readTree(matcher.group(1));
        assertThat(payload.propertyNames()).containsExactlyInAnyOrder("eventId", "userId", "occurredAt");
        assertThat(payload.get("userId").isString()).isTrue();
        assertThat(payload.get("userId").asString()).isEqualTo("123456789");
        assertThat(payload.get("occurredAt").asString()).isEqualTo("2026-10-29T08:15:30Z");
        assertThat(insert).contains("id[uuid]:'" + payload.get("eventId").asString() + "'");
    }

    @Test
    void a_call_followed_by_a_failure_rolls_the_announcement_back() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    publisher.publishDeleted(new UserId(42L), DELETED_AT);
                    throw new IllegalStateException("a later step of the deletion failed");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(probe.drainOutbox()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox", Long.class)).isZero();
    }

    @Test
    void a_call_outside_a_transaction_is_refused_and_writes_nothing() {
        assertThatThrownBy(() -> publisher.publishDeleted(new UserId(42L), DELETED_AT))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(probe.drainOutbox()).isEmpty();
    }
}

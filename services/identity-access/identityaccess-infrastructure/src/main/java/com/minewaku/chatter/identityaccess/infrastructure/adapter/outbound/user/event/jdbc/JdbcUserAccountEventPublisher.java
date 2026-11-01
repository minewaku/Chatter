package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.event.jdbc;

import com.minewaku.chatter.identityaccess.shared.port.UniqueStringIdGenerator;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.UserAccountEventPublisher;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the account-deleted announcement as a row that the change-capture relay reads from the database change
 * log. The row is removed again in the same transaction, so the table stays empty at rest. This class contains no
 * broker code; the relay is a separate process.
 */
@Component
public class JdbcUserAccountEventPublisher implements UserAccountEventPublisher {

    private static final String AGGREGATE_TYPE = "user";
    private static final String EVENT_TYPE = "UserSoftDeleted";

    private final JdbcClient jdbc;
    private final UniqueStringIdGenerator ids;
    private final ObjectMapper objectMapper;

    JdbcUserAccountEventPublisher(JdbcClient jdbc, UniqueStringIdGenerator ids, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.ids = ids;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishDeleted(UserId userId, Instant deletedAt) {
        UUID id = UUID.fromString(ids.nextId());
        String aggregateId = String.valueOf(userId.value());
        String payload = objectMapper.writeValueAsString(new UserDeletedMessage(id.toString(), aggregateId, deletedAt));

        jdbc.sql("INSERT INTO outbox (id, aggregate_type, aggregate_id, event_type, payload, occurred_at)"
                        + " VALUES (:id, :aggregateType, :aggregateId, :eventType, CAST(:payload AS jsonb), :occurredAt)")
                .param("id", id)
                .param("aggregateType", AGGREGATE_TYPE)
                .param("aggregateId", aggregateId)
                .param("eventType", EVENT_TYPE)
                .param("payload", payload)
                .param("occurredAt", deletedAt.atOffset(ZoneOffset.UTC))
                .update();
        jdbc.sql("DELETE FROM outbox WHERE id = :id").param("id", id).update();
    }
}

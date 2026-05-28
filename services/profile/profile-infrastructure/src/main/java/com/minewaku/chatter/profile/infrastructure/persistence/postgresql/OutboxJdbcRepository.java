package com.minewaku.chatter.profile.infrastructure.persistence.postgresql;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import com.minewaku.chatter.profile.infrastructure.persistence.postgresql.entity.JdbcOutboxEntity;

public interface OutboxJdbcRepository extends ListCrudRepository<JdbcOutboxEntity, UUID> {

    @Modifying
    @Query("DELETE FROM outbox WHERE created_at < :cutoffDateTime")
    void deleteByCreatedAtBefore(@Param("cutoffDateTime") Instant cutoffDateTime);
}

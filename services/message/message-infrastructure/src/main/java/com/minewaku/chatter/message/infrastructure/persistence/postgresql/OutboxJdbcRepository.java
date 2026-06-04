package com.minewaku.chatter.message.infrastructure.persistence.postgresql;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.message.infrastructure.persistence.postgresql.entity.OutboxJdbcEntity;

public interface OutboxJdbcRepository extends ListCrudRepository<OutboxJdbcEntity, UUID> {

    @Modifying
    @Query("DELETE FROM outbox WHERE created_at < :cutoffDateTime")
    void deleteByCreatedAtBefore(@Param("cutoffDateTime") Instant cutoffDateTime);
}

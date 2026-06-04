package com.minewaku.chatter.profile.infrastructure.persistence.postgresql.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("outbox")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JdbcOutboxEntity implements Persistable<UUID>{

    @Id
    private UUID id;

    @Column("aggregate_type")
    @NotBlank(message = "aggregate_type is required")
    private String aggregateType;

    @Column("aggregate_id")
    @NotBlank(message = "aggregate_id is required")
    private String aggregateId;

    @Column("event_type")
    @NotBlank(message = "event_type is required")
    private String eventType;

    @Column("payload")
    @NotNull(message = "payload cannot be null")
    //the payload is json string, we can use jsonb in postgres to store it, but since we need it to be fast, Json gonna make db converts every query
    private String payload;
    
    @Column("created_at")
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return true;
    }
}
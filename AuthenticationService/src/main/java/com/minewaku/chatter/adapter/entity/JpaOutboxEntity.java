package com.minewaku.chatter.adapter.entity;


import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JpaOutboxEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbox_seq")
    private Long id;

	@Column(name = "aggregate_type", length = 50, nullable = false, updatable = false)
	@NotBlank(message = "aggregate_type is required")
	@NotNull(message = "aggregate_type cannot be null")
    private String aggregateType;

	@Column(name = "aggregate_id", nullable = false, updatable = false)
	@NotNull(message = "aggregate_id cannot be null")
    private Long aggregateId;

	@Column(name = "event_type", length = 128, nullable = false, updatable = false)
	@NotBlank(message = "event_type is required")
	@NotNull(message = "event_type cannot be null")
    private String eventType;

    @Column(name = "payload", columnDefinition = "json", updatable = false)
    @NotNull(message = "payload cannot be null")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;
    
    @Column(name = "occurred_at", nullable = false, updatable = false)
    @NotNull(message = "occurredAt cannot be null")
    private Instant occurredAt;
    
    @Column(name = "is_processed", nullable = false)
    private boolean isProcessed;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @PrePersist
    protected void onCreate() {
    	occurredAt = Objects.isNull(occurredAt) ? Instant.now() : occurredAt;
    	isProcessed = false;
    }
}

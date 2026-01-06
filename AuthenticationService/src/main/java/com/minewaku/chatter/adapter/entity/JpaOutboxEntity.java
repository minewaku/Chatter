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

	@Column(name = "aggregate_id", length = 50 ,nullable = false, updatable = false)
    @NotBlank(message = "aggregate_id is required")
	@NotNull(message = "aggregate_id cannot be null")
    private String aggregateId;

	@Column(name = "event_type", length = 128, nullable = false, updatable = false)
	@NotBlank(message = "event_type is required")
	@NotNull(message = "event_type cannot be null")
    private String eventType;

    @Column(name = "payload", columnDefinition = "json", updatable = false)
    @NotNull(message = "payload cannot be null")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull(message = "createdAt cannot be null")
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
    	createdAt = Objects.isNull(createdAt) ? Instant.now() : createdAt;
    }
}

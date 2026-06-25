package com.minewaku.chatter.profile.infrastructure.persistence.postgresql.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("processed_event")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedEventJdbcEntity implements Persistable<UUID> {
    
    @Id
    private UUID id;

    @Column("processed_at")
    private Instant processedAt;

    @Override
    public boolean isNew() {
        return true;
    }
}

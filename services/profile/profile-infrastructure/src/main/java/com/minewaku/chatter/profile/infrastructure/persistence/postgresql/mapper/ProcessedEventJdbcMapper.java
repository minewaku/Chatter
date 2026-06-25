package com.minewaku.chatter.profile.infrastructure.persistence.postgresql.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.application.port.outbound.repository.ProcessedEventRepository;
import com.minewaku.chatter.profile.infrastructure.persistence.postgresql.entity.ProcessedEventJdbcEntity;

@Component
public class ProcessedEventJdbcMapper {

    public ProcessedEventJdbcEntity toEntity(String id, Instant processedAt) {
        return ProcessedEventJdbcEntity.builder()
                .id(UUID.fromString(id))
                .processedAt(processedAt)
                .build();
    }

    public ProcessedEventJdbcEntity toEntity(ProcessedEventRepository.ProcessedEventRecord record) {
        return toEntity(record.id(), record.processedAt());
    }

    public List<ProcessedEventJdbcEntity> toEntities(List<ProcessedEventRepository.ProcessedEventRecord> records) {
        return records.stream().map(this::toEntity).toList();
    }
}

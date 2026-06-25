package com.minewaku.chatter.message.infrastructure.persistence.postgresql;

import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.message.infrastructure.persistence.postgresql.entity.ProcessedEventJdbcEntity;

public interface ProcessedEventJdbcRepository extends ListCrudRepository<ProcessedEventJdbcEntity, UUID>{
    boolean existsById(UUID id);
}

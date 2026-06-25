package com.minewaku.chatter.profile.infrastructure.persistence.postgresql;

import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.profile.infrastructure.persistence.postgresql.entity.ProcessedEventJdbcEntity;

public interface ProcessedEventJdbcRepository extends ListCrudRepository<ProcessedEventJdbcEntity, UUID>{
    boolean existsById(UUID id);
}

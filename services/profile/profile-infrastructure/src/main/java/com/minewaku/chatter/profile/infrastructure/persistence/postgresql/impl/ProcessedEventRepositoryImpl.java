package com.minewaku.chatter.profile.infrastructure.persistence.postgresql.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.application.port.outbound.repository.ProcessedEventRepository;
import com.minewaku.chatter.profile.infrastructure.persistence.postgresql.ProcessedEventJdbcRepository;
import com.minewaku.chatter.profile.infrastructure.persistence.postgresql.mapper.ProcessedEventJdbcMapper;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ProcessedEventRepositoryImpl implements ProcessedEventRepository {

    private final ProcessedEventJdbcRepository jdbcRepository;
    private final ProcessedEventJdbcMapper mapper;

    @Override
    public boolean existsById(String id) {
        return jdbcRepository.existsById(UUID.fromString(id));
    }

    @Override
    public Set<String> findAllExistingIds(Collection<String> ids) {
        if (ids.isEmpty()) return Set.of();

        List<UUID> uuidIds = ids.stream().map(UUID::fromString).toList();

        return jdbcRepository.findAllById(uuidIds).stream()
                .map(e -> e.getId().toString())
                .collect(Collectors.toSet());
    }

    @Override
    public void save(String id, Instant processedAt) {
        jdbcRepository.save(mapper.toEntity(id, processedAt));
    }

    @Override
    public void saveAll(List<ProcessedEventRecord> records) {
        if (records.isEmpty()) return;

        jdbcRepository.saveAll(mapper.toEntities(records));
    }
}
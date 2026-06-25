package com.minewaku.chatter.message.application.port.outbound.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ProcessedEventRepository {
    boolean existsById(String id);
    Set<String> findAllExistingIds(Collection<String> ids);
    void save(String id, Instant processedAt);
    void saveAll(List<ProcessedEventRecord> records);

    public record ProcessedEventRecord(
        String id,
        Instant processedAt
    ) {
    }
}

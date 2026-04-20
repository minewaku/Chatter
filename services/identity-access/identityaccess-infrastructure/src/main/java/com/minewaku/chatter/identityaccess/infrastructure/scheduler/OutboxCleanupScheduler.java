package com.minewaku.chatter.identityaccess.infrastructure.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.JdbcOutboxRepository;

@Component
public class OutboxCleanupScheduler {

    private final JdbcOutboxRepository jdbcOutboxRepository;

    public OutboxCleanupScheduler(JdbcOutboxRepository jdbcOutboxRepository) {
        this.jdbcOutboxRepository = jdbcOutboxRepository;
    }

    @Scheduled(fixedRateString = "P1D")
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoffTime = Instant.now().minus(1, ChronoUnit.HOURS);
        jdbcOutboxRepository.deleteByCreatedAtBefore(cutoffTime);
    }

}
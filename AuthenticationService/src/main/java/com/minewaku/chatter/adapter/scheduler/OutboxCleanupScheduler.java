package com.minewaku.chatter.adapter.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.adapter.db.postgresql.JpaOutboxRepository;

@Component
public class OutboxCleanupScheduler {

    private final JpaOutboxRepository outboxRepository;

    public OutboxCleanupScheduler(JpaOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Scheduled(fixedRateString = "PT1H")
    @Transactional
    public void cleanupOldEvents() {
        outboxRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusHours(1));
    }
}
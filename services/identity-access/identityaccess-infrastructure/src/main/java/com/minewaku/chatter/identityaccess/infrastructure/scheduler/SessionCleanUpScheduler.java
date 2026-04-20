package com.minewaku.chatter.identityaccess.infrastructure.scheduler;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.JdbcSessionRepository;

@Component
public class SessionCleanUpScheduler {
    
    private final JdbcSessionRepository jdbcSessionRepository;

    public SessionCleanUpScheduler(JdbcSessionRepository jdbcSessionRepository) {
        this.jdbcSessionRepository = jdbcSessionRepository;
    }

    @Scheduled(fixedRateString = "P1D")
    @Transactional
    public void execute() {
        jdbcSessionRepository.deleteAllExpiredSessions(Instant.now());
    }
}

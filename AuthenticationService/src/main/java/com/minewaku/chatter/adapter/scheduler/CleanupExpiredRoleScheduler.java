package com.minewaku.chatter.adapter.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.adapter.db.postgresql.JpaRoleRepository;

@Component
public class CleanupExpiredRoleScheduler {
    private final JpaRoleRepository roleRepository;

    public CleanupExpiredRoleScheduler(JpaRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Scheduled(fixedRateString = "P30D")
    @Transactional
    public void cleanupOldRoles() {
        roleRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusHours(1));
    }
}

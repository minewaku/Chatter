package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity.JdbcSessionEntity;

public interface JdbcSessionRepository extends ListCrudRepository<JdbcSessionEntity, UUID> {
    List<JdbcSessionEntity> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    void deleteAllExpiredSessions(@Param("now") Instant now);

    
}

package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.repository.SessionRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.infrastructure.cache.RedisSessionReadRepository;
import com.minewaku.chatter.identityaccess.infrastructure.cache.mapper.RedisSessionMapper;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.JdbcSessionRepository;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity.JdbcSessionEntity;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.mapper.JdbcSessionMapper;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class SessionRepositoryImpl implements SessionRepository {

    private final RedisSessionReadRepository redisSessionRepository;
    private final JdbcSessionRepository jdbcSessionRepository;
    private final JdbcSessionMapper jdbcSessionMapper;
    private final RedisSessionMapper redisSessionMapper;

    @Override
    public void save(Session session) {
        JdbcSessionEntity entity = jdbcSessionRepository.save(jdbcSessionMapper.domainToEntity(session));
        redisSessionRepository.save(redisSessionMapper.entityToModel(entity));
    }

    @Override
    public Optional<Session> findById(SessionId sessionId) {
        return jdbcSessionMapper.entityToDomain(jdbcSessionRepository.findById(UUID.fromString(sessionId.getValue())));
    }

    @Override
    public List<Session> findByUserId(UserId userId) {
        return jdbcSessionMapper.entityListToDomainList(jdbcSessionRepository.findByUserId((userId.getValue())));
    }

    @Override
    public void saveAll(List<Session> sessions) {
        jdbcSessionRepository.saveAll(jdbcSessionMapper.domainListToEntityList(sessions));
    }
    
}

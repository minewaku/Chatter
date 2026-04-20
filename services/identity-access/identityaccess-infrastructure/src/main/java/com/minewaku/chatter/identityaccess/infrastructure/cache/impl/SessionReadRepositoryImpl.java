package com.minewaku.chatter.identityaccess.infrastructure.cache.impl;

import java.util.Set;

import org.springframework.stereotype.Repository;

import com.minewaku.chatter.identityaccess.application.port.outbound.query.SessionReadRepository;
import com.minewaku.chatter.identityaccess.application.port.outbound.query.model.SessionReadModel;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.infrastructure.cache.RedisSessionReadRepository;

@Repository
public class SessionReadRepositoryImpl implements SessionReadRepository {

    private final RedisSessionReadRepository redisSessionReadRepository;

    public SessionReadRepositoryImpl(
                RedisSessionReadRepository redisSessionReadRepository) {

        this.redisSessionReadRepository = redisSessionReadRepository;
    }

    @Override
    public Set<SessionReadModel> findAllSessionsByUserId(UserId userId) {
        Set<SessionReadModel > sessionCacheDtos = redisSessionReadRepository.findAllSessionsByUserId(userId.getValue().toString());
        return sessionCacheDtos.stream()
                .map(dto -> new SessionReadModel(
                        dto.sessionId(),
                        dto.userId(),
                        dto.deviceInfo(),
                        dto.issuedAt(),
                        dto.expiresAt(),
                        dto.lastRefreshedAt()))
                .collect(java.util.stream.Collectors.toSet());
    }
    
}

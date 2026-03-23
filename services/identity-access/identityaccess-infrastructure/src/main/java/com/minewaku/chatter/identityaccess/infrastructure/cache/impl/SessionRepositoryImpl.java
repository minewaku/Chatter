package com.minewaku.chatter.identityaccess.infrastructure.cache.impl;

import java.util.Optional;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.repository.SessionRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.infrastructure.cache.RedisSessionRepository;
import com.minewaku.chatter.identityaccess.infrastructure.cache.mapper.SessionMapper;

public class SessionRepositoryImpl implements SessionRepository {

    private RedisSessionRepository redisSessionRepository;
    private SessionMapper sessionMapper;

    public SessionRepositoryImpl(
            RedisSessionRepository redisSessionRepository,
            SessionMapper sessionMapper) {
                
        this.redisSessionRepository = redisSessionRepository;
        this.sessionMapper = sessionMapper;
    }

    @Override
    public void save(Session session) {
        redisSessionRepository.save(sessionMapper.domainToDto(session));
    }

    @Override
    public void delete(Session session) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
    }

    @Override
    public void deleteById(SessionId sessionId) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
    }

    @Override
    public void deleteByOtherSessionByUserId(UserId userId, SessionId sessionId) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteByOtherSessionByUserId'");
    }

    @Override
    public Optional<Session> findById(SessionId sessionId) {
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }
    
}

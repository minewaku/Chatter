package com.minewaku.chatter.identityaccess.domain.aggregate.session.repository;

import java.util.Optional;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

public interface SessionRepository {
    void save(Session session);
    void delete(Session session);
    void deleteById(SessionId sessionId);
    void deleteByOtherSessionByUserId(UserId userId, SessionId sessionId);
    Optional<Session> findById(SessionId sessionId);
}

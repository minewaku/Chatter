package com.minewaku.chatter.identityaccess.domain.aggregate.session.repository;

import java.util.List;
import java.util.Optional;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

public interface SessionRepository {
    void save(Session session);
    Optional<Session> findById(SessionId sessionId);
    List<Session> findByUserId(UserId userId);
    void saveAll(List<Session> sessions);
}

package com.minewaku.chatter.identityaccess.application.messaging.subcriber.domain;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.messaging.subcriber.core.DomainEventSubscriber;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.repository.SessionRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

@Service
public class UserSoftDeletedDomainEventSubcriber implements DomainEventSubscriber<UserSoftDeletedDomainEvent> {

    private final SessionRepository sessionRepository;

    public UserSoftDeletedDomainEventSubcriber(
                SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional
    public void handle(UserSoftDeletedDomainEvent event) {
        List<Session> sessions = sessionRepository.findByUserId(new UserId(Long.parseLong(event.getUserId())));
        
        sessions.forEach(session -> {
            session.revoke();
        });

        sessionRepository.saveAll(sessions);
    }
    
}

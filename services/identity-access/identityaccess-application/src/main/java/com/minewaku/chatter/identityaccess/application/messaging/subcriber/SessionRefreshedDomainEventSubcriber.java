package com.minewaku.chatter.identityaccess.application.messaging.subcriber;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.messaging.subcriber.core.DomainEventSubscriber;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.event.SessionRefreshedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;

import io.github.resilience4j.retry.annotation.Retry;

public class SessionRefreshedDomainEventSubcriber implements DomainEventSubscriber<SessionRefreshedDomainEvent>{
    
    private final UserRepository userRepository;

    public SessionRefreshedDomainEventSubcriber(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public void handle(SessionRefreshedDomainEvent event) {

        UserId userId = new UserId(Long.parseLong(event.getUserId()));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.refreshLastLogin();
    }
}
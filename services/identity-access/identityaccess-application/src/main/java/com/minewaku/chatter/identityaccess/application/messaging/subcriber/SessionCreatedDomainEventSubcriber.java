package com.minewaku.chatter.identityaccess.application.messaging.subcriber;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.messaging.subcriber.core.DomainEventSubscriber;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.event.SessionCreatedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;

import io.github.resilience4j.retry.annotation.Retry;

public class SessionCreatedDomainEventSubcriber implements DomainEventSubscriber<SessionCreatedDomainEvent>{
    
    private final UserRepository userRepository;

    public SessionCreatedDomainEventSubcriber(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public void handle(SessionCreatedDomainEvent event) {

        UserId userId = new UserId(Long.parseLong(event.getUserId()));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.refreshLastLogin();
    }
}

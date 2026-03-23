package com.minewaku.chatter.identityaccess.application.messaging.subcriber;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.DataInconsistencyException;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.core.DomainEventSubscriber;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenVerifiedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;

import io.github.resilience4j.retry.annotation.Retry;

public class ConfirmationTokenVerifiedDomainEventSubcriber implements DomainEventSubscriber<ConfirmationTokenVerifiedDomainEvent> {
    
    private final UserRepository userRepository;

	public ConfirmationTokenVerifiedDomainEventSubcriber(
        UserRepository userRepository) {

        this.userRepository = userRepository;
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public void handle(ConfirmationTokenVerifiedDomainEvent event) {

		User user = userRepository.findById(new UserId(Long.valueOf(event.getUserId())))
			.orElseThrow(() -> new DataInconsistencyException(
				String.format(
					"Data Integrity Violation: ConfirmationToken '%s' exists, but associated User '%s' is missing from database.",
					event.getToken(),
					event.getUserId()
				)
        ));

        user.enable();
        userRepository.save(user);
	}
}

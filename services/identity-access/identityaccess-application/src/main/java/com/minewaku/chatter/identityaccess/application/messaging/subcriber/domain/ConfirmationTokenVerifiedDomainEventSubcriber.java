package com.minewaku.chatter.identityaccess.application.messaging.subcriber.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.DataInconsistencyException;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.EnablementUpdatedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.core.DomainEventSubscriber;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenVerifiedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ConfirmationTokenVerifiedDomainEventSubcriber implements DomainEventSubscriber<ConfirmationTokenVerifiedDomainEvent> {
    
    private final UserRepository userRepository;
	private final UniqueStringIdGenerator uniqueStringIdGenerator;
	private final IntegrationEventPublisher integrationEventPublisher;

	public ConfirmationTokenVerifiedDomainEventSubcriber(
        UserRepository userRepository,
		UniqueStringIdGenerator uniqueStringIdGenerator,
		OutboxStore outboxStore) {

        this.userRepository = userRepository;
		this.uniqueStringIdGenerator = uniqueStringIdGenerator;
		this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public void handle(ConfirmationTokenVerifiedDomainEvent domainEvent) {

		User user = userRepository.findById(new UserId(Long.valueOf(domainEvent.getUserId())))
			.orElseThrow(() -> new DataInconsistencyException(
				String.format(
					"Data Integrity Violation: ConfirmationToken '%s' exists, but associated User '%s' is missing from database.",
					domainEvent.getToken(),
					domainEvent.getUserId()
				)
        ));

        if (user.enable()) {
            userRepository.save(user);

			String eventId = uniqueStringIdGenerator.generate();
			EnablementUpdatedIntegrationEvent event = new EnablementUpdatedIntegrationEvent(
				user.getId().getValue(), user.getEnablement().isEnabled(), 
				user.getEnablement().isLocked(), 
				user.getEnablement().getDeletionStatus().isDeleted(), 
				user.getEnablement().getDeletionStatus().getDeletedAt()
			);
			IntegrationEventWrapper<EnablementUpdatedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, user.getId().getValue().toString(), event);
			integrationEventPublisher.publish(wrapper);
        } 
	}
}
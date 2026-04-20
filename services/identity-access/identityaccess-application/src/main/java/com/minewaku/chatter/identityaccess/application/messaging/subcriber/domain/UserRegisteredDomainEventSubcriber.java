package com.minewaku.chatter.identityaccess.application.messaging.subcriber.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.ConfirmationTokenCreatedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.core.DomainEventSubscriber;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserRegisteredDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class UserRegisteredDomainEventSubcriber implements DomainEventSubscriber<UserRegisteredDomainEvent> {
    
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;

    private final IntegrationEventPublisher integrationEventPublisher;

	public UserRegisteredDomainEventSubcriber(
        ConfirmationTokenRepository confirmationTokenRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,

        OutboxStore outboxStore) {

		this.confirmationTokenRepository = confirmationTokenRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;

        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public void handle(UserRegisteredDomainEvent event) {
        String token = uniqueStringIdGenerator.generate();
        ConfirmationToken confirmationToken = ConfirmationToken.createNew(
            token,
            new UserId(Long.parseLong(event.getUserId())),
            new Email(event.getEmail()),
            null
        );
        confirmationTokenRepository.save(confirmationToken);

        String eventId = uniqueStringIdGenerator.generate();
        ConfirmationTokenCreatedIntegrationEvent confirmationTokenCreatedIntegrationEvent = new ConfirmationTokenCreatedIntegrationEvent(
            token,
            confirmationToken.getUserId().getValue(),
            confirmationToken.getEmail().getValue(),
            confirmationToken.getDuration().toString(),
            confirmationToken.getExpiresAt().toString()
        );
        IntegrationEventWrapper<ConfirmationTokenCreatedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, confirmationToken.getToken(), confirmationTokenCreatedIntegrationEvent);
        integrationEventPublisher.publish(wrapper);
	}
}

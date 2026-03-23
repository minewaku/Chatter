package com.minewaku.chatter.identityaccess.application.messaging.subcriber;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.core.DomainEventSubscriber;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.ConfirmationTokenGenerator;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenCreatedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserRegisteredDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

import io.github.resilience4j.retry.annotation.Retry;


public class UserRegisteredDomainEventSubcriber implements DomainEventSubscriber<UserRegisteredDomainEvent> {
    
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final ConfirmationTokenGenerator confirmationTokenGenerator;
    private final DomainEventPublisher domainEventPublisher;

	public UserRegisteredDomainEventSubcriber(
        ConfirmationTokenRepository confirmationTokenRepository,
        ConfirmationTokenGenerator confirmationTokenGenerator,
        SyncDomainEventPublisher syncDomainEventPublisher) {

		this.confirmationTokenRepository = confirmationTokenRepository;
        this.confirmationTokenGenerator = confirmationTokenGenerator;
        this.domainEventPublisher = syncDomainEventPublisher;
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public void handle(UserRegisteredDomainEvent event) {
        String token = confirmationTokenGenerator.generate();
        ConfirmationToken confirmationToken = ConfirmationToken.createNew(
            token,
            new UserId(Long.parseLong(event.getUserId())),
            new Email(event.getEmail()),
            null
        );

        List<DomainEvent> filteredEvents = confirmationTokenCreatedDomainEventFiltered(confirmationToken.getEvents());
        domainEventPublisher.publish(filteredEvents);

        confirmationTokenRepository.save(confirmationToken);
	}

    private List<DomainEvent> confirmationTokenCreatedDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof ConfirmationTokenCreatedDomainEvent)
                .toList();
    }
}

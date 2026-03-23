package com.minewaku.chatter.identityaccess.application.service.command.confirmationtoken;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventQueue;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase.VerifyConfirmationTokenUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenVerifiedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

import io.github.resilience4j.retry.annotation.Retry;

public class VerifyConfirmationTokenApplicationService implements VerifyConfirmationTokenUseCase {
	
	private final ConfirmationTokenRepository confirmationTokenRepository;
	private final DomainEventPublisher domainEventPublisher;

	public VerifyConfirmationTokenApplicationService(
			ConfirmationTokenRepository confirmationTokenRepository,
			SyncDomainEventQueue syncDomainEventQueue) {	
		
		this.confirmationTokenRepository = confirmationTokenRepository;
		this.domainEventPublisher = new SyncDomainEventPublisher(syncDomainEventQueue);
	}
	
    @Override
	@Retry(name = "transientDataAccess")
    @Transactional
    public Void handle(String confirmationToken) {
		ConfirmationToken existConfirmationToken = confirmationTokenRepository.findByToken(confirmationToken)
				.orElseThrow(() -> new EntityNotFoundException("Token does not exist"));

		existConfirmationToken.verifyToken();
		confirmationTokenRepository.save(existConfirmationToken);

		List<DomainEvent> filteredEvents = filterEvents(existConfirmationToken.getEvents());
		domainEventPublisher.publish(filteredEvents);

        return null;
	}
	
	private List<DomainEvent> filterEvents(List<DomainEvent> events) {
	    return events.stream()
	            .filter(event -> event.getClass().equals(ConfirmationTokenVerifiedDomainEvent.class))
	            .collect(Collectors.toList());
	}
}

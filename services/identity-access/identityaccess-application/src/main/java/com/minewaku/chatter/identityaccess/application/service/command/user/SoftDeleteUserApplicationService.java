package com.minewaku.chatter.identityaccess.application.service.command.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.EventQueue;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserSoftDeletedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase.SoftDeleteUserUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class SoftDeleteUserApplicationService implements SoftDeleteUserUseCase {
	
	private final UserRepository userRepository;

	private final UniqueStringIdGenerator uniqueStringIdGenerator;

	private final DomainEventPublisher domainEventPublisher;
	private final IntegrationEventPublisher integrationEventPublisher;
	

	public SoftDeleteUserApplicationService(
				UserRepository userRepository,

				UniqueStringIdGenerator uniqueStringIdGenerator,

				EventQueue eventQueue,
				OutboxStore outboxStore) {


		this.userRepository = userRepository;

		this.uniqueStringIdGenerator = uniqueStringIdGenerator;

		this.domainEventPublisher = new DomainEventPublisher(eventQueue);
		this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
	}


    @Override
	@Retry(name = "transientDataAccess")
	@Transactional
    public Void handle(UserId userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		
		if(user.softDelete()) {
			userRepository.save(user);

			List<DomainEvent> filteredEvents = userSoftDeletedDomainEventFiltered(user.getEvents());
			domainEventPublisher.publish(filteredEvents);

			String eventId = uniqueStringIdGenerator.generate();
			UserSoftDeletedIntegrationEvent event = new UserSoftDeletedIntegrationEvent(
				user.getId().getValue(), 
				user.getEmail().getValue(), 
				user.getUsername().getValue(), 
				user.getBirthday().getValue(), 
				user.getEnablement().isEnabled(), 
				user.getEnablement().isLocked(), 
				user.getEnablement().getDeletionStatus().isDeleted(), 
				user.getEnablement().getDeletionStatus().getDeletedAt());
			IntegrationEventWrapper<UserSoftDeletedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, user.getId().getValue().toString(), event);
			integrationEventPublisher.publish(wrapper);
		}

        return null;
	}

	private List<DomainEvent> userSoftDeletedDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof UserSoftDeletedDomainEvent)
                .toList();
    }
}

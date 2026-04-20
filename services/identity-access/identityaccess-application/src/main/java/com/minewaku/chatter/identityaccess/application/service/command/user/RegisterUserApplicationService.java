package com.minewaku.chatter.identityaccess.application.service.command.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.EventQueue;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserRegisteredIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.RegisterCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserRegisteredDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.service.RegisterDomainService;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class RegisterUserApplicationService implements RegisterUserUseCase {
    
	private final UserRepository userRepository;

	private final RegisterDomainService registerDomainService;

	private final UniqueStringIdGenerator uniqueStringIdGenerator;
	
	private final DomainEventPublisher domainEventPublisher;
	private final IntegrationEventPublisher integrationEventPublisher;


	public RegisterUserApplicationService(
			UserRepository userRepository,

			RegisterDomainService registerDomainService,

			UniqueStringIdGenerator uniqueStringIdGenerator,

			EventQueue eventQueue,
			OutboxStore outboxStore) {


		this.userRepository = userRepository;

		this.registerDomainService = registerDomainService;

		this.uniqueStringIdGenerator = uniqueStringIdGenerator;

		this.domainEventPublisher = new DomainEventPublisher(eventQueue);
		this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
	}


	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public Void handle(RegisterCommand command) {

		User user = registerDomainService.handle(
			command.email(),
			command.username(),
			command.birthday(),
			command.password());
			
		userRepository.save(user);

		List<DomainEvent> filteredEvents = userRegisteredDomainEventFiltered(user.getEvents());
        domainEventPublisher.publish(filteredEvents);
		
		String eventId = uniqueStringIdGenerator.generate();
		UserRegisteredIntegrationEvent event = new UserRegisteredIntegrationEvent(
			user.getId().getValue(),
			user.getEmail().getValue(),
			user.getUsername().getValue(),
			user.getBirthday().getValue(),
			user.getEnablement().isEnabled(),
			user.getEnablement().getDeletionStatus().isDeleted(),
			user.getEnablement().isLocked(),
			user.getEnablement().getDeletionStatus().getDeletedAt());

		IntegrationEventWrapper<UserRegisteredIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, user.getId().getValue().toString(), event);
		integrationEventPublisher.publish(wrapper);

		return null;
	}

	private List<DomainEvent> userRegisteredDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof UserRegisteredDomainEvent)
                .toList();
    }
}

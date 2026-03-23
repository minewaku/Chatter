package com.minewaku.chatter.identityaccess.application.service.command.user;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventQueue;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserRegisteredIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.RegisterCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.PasswordHasher;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserRegisteredDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

public class RegisterUserApplicationService implements RegisterUserUseCase {
    
	private final UserRepository userRepository;

	private final PasswordHasher passwordHasher;
	private final TimeBasedIdGenerator timeBasedIdGenerator;
	private final UniqueStringIdGenerator uniqueStringIdGenerator;
	
	private final DomainEventPublisher domainEventPublisher;
	private final IntegrationEventPublisher integrationEventPublisher;


	public RegisterUserApplicationService(
			UserRepository userRepository,

			PasswordHasher passwordHasher,
			TimeBasedIdGenerator timeBasedIdGenerator,
			UniqueStringIdGenerator uniqueStringIdGenerator,

			SyncDomainEventQueue syncDomainEventQueue,
			OutboxStore outboxStore) {


		this.userRepository = userRepository;
		
		this.passwordHasher = passwordHasher;
		this.timeBasedIdGenerator = timeBasedIdGenerator;
		this.uniqueStringIdGenerator = uniqueStringIdGenerator;

		this.domainEventPublisher = new SyncDomainEventPublisher(syncDomainEventQueue);
		this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
	}


	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public Void handle(RegisterCommand command) {

		UserId userId = new UserId(timeBasedIdGenerator.generate());
		HashedPassword hashedPassword = passwordHasher.hash(command.password());

		User user = User.register(
				userId,
				command.email(),
				command.username(),
				command.birthday(),
				hashedPassword);

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

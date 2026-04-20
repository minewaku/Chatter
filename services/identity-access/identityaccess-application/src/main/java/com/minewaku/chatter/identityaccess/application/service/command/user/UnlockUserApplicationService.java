package com.minewaku.chatter.identityaccess.application.service.command.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.EnablementUpdatedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase.UnlockUserUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class UnlockUserApplicationService implements UnlockUserUseCase {

	private final UserRepository userRepository;

	private final UniqueStringIdGenerator uniqueStringIdGenerator;
	private final IntegrationEventPublisher integrationEventPublisher;


	public UnlockUserApplicationService(
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
	public Void handle(UserId userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		
		if(user.unlock()) {
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

		return null;
	}
}

package com.minewaku.chatter.identityaccess.application.service.command.user;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserHardDeletedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase.HardDeleteUserUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

public class HardDeleteUserApplicationService implements HardDeleteUserUseCase {
	
	private final UserRepository userRepository;
	private final UniqueStringIdGenerator uniqueStringIdGenerator;
	private final IntegrationEventPublisher integrationEventPublisher;
	
	
	public HardDeleteUserApplicationService(
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
		Optional<User> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) return null;
    	User user = userOpt.get();	

        userRepository.deleteById(userId);

		String eventId = uniqueStringIdGenerator.generate();
		UserHardDeletedIntegrationEvent event = new UserHardDeletedIntegrationEvent(user.getId().getValue().toString());
		IntegrationEventWrapper<UserHardDeletedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, user.getId().getValue().toString(), event);
		integrationEventPublisher.publish(wrapper);

        return null;
	}
}

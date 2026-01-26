package com.minewaku.chatter.application.service.user;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.publisher.StoreEvent;
import com.minewaku.chatter.application.publisher.StoreEventPublisher;
import com.minewaku.chatter.domain.event.UserHardDeletedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.user.HardDeleteUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

import io.github.resilience4j.retry.annotation.Retry;

public class HardDeleteUserApplicationService implements HardDeleteUserUseCase {
	
	private final UserRepository userRepository;
	private final StoreEventPublisher storeEventPublisher;
	
	
	public HardDeleteUserApplicationService(
			UserRepository userRepository, 
			StoreEvent storeEvent) {
		
		this.userRepository = userRepository;
		this.storeEventPublisher = new StoreEventPublisher(storeEvent);
	}
	
	
    @Override
	@Retry(name = "transientDataAccess")
	@Transactional
    public Void handle(UserId userId) {
		Optional<User> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) return null;
    	User user = userOpt.get();	

        userRepository.hardDeleteById(userId);

		List<DomainEvent> filterEvents = filterEvents(user.getEvents());
		storeEventPublisher.publish(filterEvents);
        return null;
	}

	private List<DomainEvent> filterEvents(List<DomainEvent> events) {
		return events.stream()
				.filter(event -> event.getClass().equals(UserHardDeletedDomainEvent.class))
				.collect(Collectors.toList());
	}
}

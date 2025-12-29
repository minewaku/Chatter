package com.minewaku.chatter.application.service.user;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.application.publisher.StoreEvent;
import com.minewaku.chatter.application.publisher.StoreEventPublisher;
import com.minewaku.chatter.domain.event.UserUnlockedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.user.UnlockUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

public class UnlockUserApplicationService implements UnlockUserUseCase {

	private final UserRepository userRepository;
	private final StoreEventPublisher storeEventPublisher;

	public UnlockUserApplicationService(UserRepository userRepository, StoreEvent storeEvent) {
		this.userRepository = userRepository;
		this.storeEventPublisher = new StoreEventPublisher(storeEvent);
	}

	@Override
	@Transactional
	public Void handle(UserId userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		user.unlock();

		userRepository.unlock(user);

		List<DomainEvent> filterEvents = filterEventsSendToken(user.getEvents());
		storeEventPublisher.publish(filterEvents);
		
		return null;
	}

	private List<DomainEvent> filterEventsSendToken(List<DomainEvent> events) {
		return events.stream()
				.filter(event -> event.getClass().equals(UserUnlockedDomainEvent.class))
				.collect(Collectors.toList());
	}
}

package com.minewaku.chatter.application.service.user;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.application.publisher.StoreEvent;
import com.minewaku.chatter.application.publisher.StoreEventPublisher;
import com.minewaku.chatter.domain.event.UserLockedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.user.LockUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

import io.github.resilience4j.retry.annotation.Retry;

public class LockUserApplicationService implements LockUserUseCase {

	private final UserRepository userRepository;
	private final StoreEventPublisher storeEventPublisher;

	public LockUserApplicationService(UserRepository userRepository, StoreEvent storeEvent) {
		this.userRepository = userRepository;
		this.storeEventPublisher = new StoreEventPublisher(storeEvent);
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public Void handle(UserId userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		user.lock();

		userRepository.lock(user);

		List<DomainEvent> filterEvents = filterEvents(user.getEvents());
		storeEventPublisher.publish(filterEvents);
		return null;
	}

	private List<DomainEvent> filterEvents(List<DomainEvent> events) {
		return events.stream()
				.filter(event -> event.getClass().equals(UserLockedDomainEvent.class))
				.collect(Collectors.toList());
	}
}

package com.minewaku.chatter.application.service.user;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.application.publisher.StoreEvent;
import com.minewaku.chatter.application.publisher.StoreEventPublisher;
import com.minewaku.chatter.domain.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.user.SoftDeleteUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

public class SoftDeleteUserApplicationService implements SoftDeleteUserUseCase {
	
	private final UserRepository userRepository;
	private final StoreEventPublisher storeEventPublisher;
	
	public SoftDeleteUserApplicationService(UserRepository userRepository, StoreEvent storeEvent) {
		this.userRepository = userRepository;
		this.storeEventPublisher = new StoreEventPublisher(storeEvent);
	}

    @Override
	@Transactional
    public Void handle(UserId userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		user.softDelete();

        userRepository.softDelete(user);

		List<DomainEvent> filterEvents = filterEventsSendToken(user.getEvents());
		storeEventPublisher.publish(filterEvents);
		
        return null;
	}

	private List<DomainEvent> filterEventsSendToken(List<DomainEvent> events) {
		return events.stream()
				.filter(event -> event.getClass().equals(UserSoftDeletedDomainEvent.class))
				.collect(Collectors.toList());
	}
}

package com.minewaku.chatter.application.subcriber;

import java.util.List;
import java.util.stream.Collectors;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.application.publisher.QueueEventPublisher;
import com.minewaku.chatter.domain.event.CreateConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.SendConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEventSubscriber;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.service.KeyGenerator;

public class CreateConfirmationTokenDomainEventSubcriber implements DomainEventSubscriber<CreateConfirmationTokenDomainEvent> {
	
	private final ConfirmationTokenRepository confirmationTokenRepository;
	private final UserRepository userRepository;
	private final KeyGenerator keyGenerator;
	
	private final QueueEventPublisher queueEventPublisher;
	
	public CreateConfirmationTokenDomainEventSubcriber(ConfirmationTokenRepository confirmationTokenRepository,
			UserRepository userRepository,
			KeyGenerator keyGenerator,
			MessageQueue messageQueue) {
		
		this.confirmationTokenRepository = confirmationTokenRepository;
		this.userRepository = userRepository;
		this.keyGenerator = keyGenerator;
		
		this.queueEventPublisher = new QueueEventPublisher(messageQueue);
	}

	@Override
	public void handle(CreateConfirmationTokenDomainEvent event) {
		String key = keyGenerator.generate();
		User user = userRepository.findByIdAndIsDeletedFalse(event.getUserId()).orElseThrow(
				() -> new EntityNotFoundException("User does not exist"));
		
		ConfirmationToken confirmationToken = ConfirmationToken.createNew(key, event.getUserId(), user.getEmail(), event.getDuration());
		confirmationTokenRepository.save(confirmationToken);
		
		List<DomainEvent> filteredEvents = filterEvents(confirmationToken.getEvents());
		queueEventPublisher.publish(filteredEvents);
	}
	
	private List<DomainEvent> filterEvents(List<DomainEvent> events) {
	    return events.stream()
	            .filter(event -> event.getClass().equals(SendConfirmationTokenDomainEvent.class))
	            .collect(Collectors.toList());
	}

}

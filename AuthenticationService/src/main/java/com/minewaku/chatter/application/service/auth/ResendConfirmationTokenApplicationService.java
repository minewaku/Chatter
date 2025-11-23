package com.minewaku.chatter.application.service.auth;

import java.util.List;
import java.util.stream.Collectors;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.application.publisher.QueueEventPublisher;
import com.minewaku.chatter.domain.event.SendConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.auth.ResendConfirmationTokenUseCase;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.service.KeyGenerator;
import com.minewaku.chatter.domain.value.Email;

public class ResendConfirmationTokenApplicationService implements ResendConfirmationTokenUseCase {
	
	private final ConfirmationTokenRepository confirmationTokenRepository;
	private final UserRepository userRepository;
	private final KeyGenerator keyGenerator;
	
	private final QueueEventPublisher queueEventPublisher;
	
	
	public ResendConfirmationTokenApplicationService(ConfirmationTokenRepository confirmationTokenRepository,
			UserRepository userRepository,
			KeyGenerator keyGenerator,
			
			MessageQueue messageQueue) {
		this.confirmationTokenRepository = confirmationTokenRepository;
		this.userRepository = userRepository;
		this.keyGenerator = keyGenerator;
		
		this.queueEventPublisher = new QueueEventPublisher(messageQueue);
	}
	
	
	@Override
	public Void handle(Email email) {
		confirmationTokenRepository.deleteByEmail(email);
		
		User user = userRepository.findByEmailAndIsDeletedFalse(email)
				.orElseThrow(() -> new EntityNotFoundException("User does not exist"));

		user.checkForDisable();
		String key = keyGenerator.generate();
		
		ConfirmationToken confirmationToken = ConfirmationToken.createNew(key, user.getId(), email, null);
		List<DomainEvent> filteredEvents = filterEvents(confirmationToken.getEvents());
		queueEventPublisher.publish(filteredEvents);
		
		return null;
	}
	
	private List<DomainEvent> filterEvents(List<DomainEvent> events) {
	    return events.stream()
	            .filter(event -> event.getClass().equals(SendConfirmationTokenDomainEvent.class))
	            .collect(Collectors.toList());
	}

}

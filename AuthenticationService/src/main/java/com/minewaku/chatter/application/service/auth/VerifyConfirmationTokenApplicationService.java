package com.minewaku.chatter.application.service.auth;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.application.publisher.QueueEventPublisher;
import com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.auth.VerifyConfirmationTokenUseCase;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;

public class VerifyConfirmationTokenApplicationService implements VerifyConfirmationTokenUseCase {

	private final ConfirmationTokenRepository confirmationTokenRepository;
	private final UserRepository userRepository;
	
	private final QueueEventPublisher queueEventPublisher;
	
	
	public VerifyConfirmationTokenApplicationService(
			ConfirmationTokenRepository confirmationTokenRepository,
			UserRepository userRepository,
			MessageQueue messageQueue) {	
		
		this.confirmationTokenRepository = confirmationTokenRepository;
		this.userRepository = userRepository;
		
		this.queueEventPublisher = new QueueEventPublisher(messageQueue);
	}
	
    @Override
    @Transactional
    public Void handle(String confirmationToken) {
		ConfirmationToken existConfirmationToken = confirmationTokenRepository.findByToken(confirmationToken)
				.orElseThrow(() -> new EntityNotFoundException("Token does not exist"));
		
		User user = userRepository.findByIdAndIsDeletedFalse(existConfirmationToken.getUserId())
				.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		
		confirmationTokenRepository.confirmEmail(existConfirmationToken);
		userRepository.enable(user);
		
		List<DomainEvent> filterEvents = filterEvents(user.getEvents());
        queueEventPublisher.publish(filterEvents);
        return null;
	}
	
	private List<DomainEvent> filterEvents(List<DomainEvent> events) {
	    return events.stream()
	            .filter(event -> event.getClass().equals(AccountVerifiedDomainEvent.class))
	            .collect(Collectors.toList());
	}

}

package com.minewaku.chatter.application.service.confirmation_token;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.application.publisher.QueueEventPublisher;
import com.minewaku.chatter.domain.command.confirmation_token.CreateConfirmationTokenCommand;
import com.minewaku.chatter.domain.event.SendConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.confirmation_token.CreateConfirmationTokenUseCase;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.service.ConfirmationTokenGenerator;

public class CreateConfirmationTokenApplicationService implements CreateConfirmationTokenUseCase {

	private final ConfirmationTokenRepository confirmationTokenRepository;
	private final UserRepository userRepository;
	private final ConfirmationTokenGenerator keyGenerator;

	private final QueueEventPublisher queueEventPublisher;

	public CreateConfirmationTokenApplicationService(ConfirmationTokenRepository confirmationTokenRepository,
			UserRepository userRepository,
			ConfirmationTokenGenerator keyGenerator,
			MessageQueue messageQueue) {

		this.confirmationTokenRepository = confirmationTokenRepository;
		this.userRepository = userRepository;
		this.keyGenerator = keyGenerator;

		this.queueEventPublisher = new QueueEventPublisher(messageQueue);
	}

	@Override
	@Transactional
	public ConfirmationToken handle(CreateConfirmationTokenCommand command) {
		String key = keyGenerator.generate();
		User user = userRepository.findById(command.getUserId()).orElseThrow(
				() -> new EntityNotFoundException("User does not exist"));
		user.validateAccessible();

		ConfirmationToken confirmationToken = ConfirmationToken.createNew(key, command.getUserId(), user.getEmail(),
				command.getDuration());
		ConfirmationToken newConfirmationToken = confirmationTokenRepository.save(confirmationToken);

		List<DomainEvent> filteredEvents = filterEvents(confirmationToken.getEvents());
		queueEventPublisher.publish(filteredEvents);

		return newConfirmationToken;
	}

	private List<DomainEvent> filterEvents(List<DomainEvent> events) {
		return events.stream()
				.filter(event -> event.getClass().equals(SendConfirmationTokenDomainEvent.class))
				.collect(Collectors.toList());
	}

}

package com.minewaku.chatter.identityaccess.application.service.command.confirmationtoken;

import java.util.List;
import java.util.stream.Collectors;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventQueue;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase.ResendConfirmationTokenUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenCreatedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.service.ResendConfirmationTokenDomainService;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

public class ResendConfirmationTokenApplicationService implements ResendConfirmationTokenUseCase {

    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final UserRepository userRepository;

    private final DomainEventPublisher domainEventPublisher;

    private final ResendConfirmationTokenDomainService resendConfirmationTokenDomainService;

    public ResendConfirmationTokenApplicationService(
                ConfirmationTokenRepository confirmationTokenRepository,
                UserRepository userRepository,

                SyncDomainEventQueue syncDomainEventQueue,
            
                ResendConfirmationTokenDomainService resendConfirmationTokenDomainService) {

                    
        this.confirmationTokenRepository = confirmationTokenRepository;
        this.userRepository = userRepository;

        this.domainEventPublisher = new SyncDomainEventPublisher(syncDomainEventQueue);
        
        this.resendConfirmationTokenDomainService = resendConfirmationTokenDomainService;
    }

    @Override
    public Void handle(Email email) {
        confirmationTokenRepository.deleteByEmail(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ConfirmationToken confirmationToken = resendConfirmationTokenDomainService.handle(user);
        
        List<DomainEvent> filteredEvents = filterEvents(confirmationToken.getEvents());
		domainEventPublisher.publish(filteredEvents);

        return null;
    }

    private List<DomainEvent> filterEvents(List<DomainEvent> events) {
	    return events.stream()
	            .filter(event -> event.getClass().equals(ConfirmationTokenCreatedDomainEvent.class))
	            .collect(Collectors.toList());
	}
    
}

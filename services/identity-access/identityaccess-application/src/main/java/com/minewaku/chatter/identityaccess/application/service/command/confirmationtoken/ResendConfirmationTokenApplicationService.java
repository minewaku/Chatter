package com.minewaku.chatter.identityaccess.application.service.command.confirmationtoken;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.ConfirmationTokenCreatedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase.ResendConfirmationTokenUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.service.ResendConfirmationTokenDomainService;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ResendConfirmationTokenApplicationService implements ResendConfirmationTokenUseCase {

    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final UserRepository userRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;

    private final IntegrationEventPublisher integrationEventPublisher;

    private final ResendConfirmationTokenDomainService resendConfirmationTokenDomainService;

    public ResendConfirmationTokenApplicationService(
                ConfirmationTokenRepository confirmationTokenRepository,
                UserRepository userRepository,
                UniqueStringIdGenerator uniqueStringIdGenerator,
                

                OutboxStore outboxStore,
            
                ResendConfirmationTokenDomainService resendConfirmationTokenDomainService) {

                    
        this.confirmationTokenRepository = confirmationTokenRepository;
        this.userRepository = userRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;

        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
        
        this.resendConfirmationTokenDomainService = resendConfirmationTokenDomainService;
    }

    @Override
    @Retry(name = "transientDataAccess")
    @Transactional
    public Void handle(Email email) {
        confirmationTokenRepository.deleteByEmail(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ConfirmationToken confirmationToken = resendConfirmationTokenDomainService.handle(user);
        confirmationTokenRepository.save(confirmationToken);

        String eventId = uniqueStringIdGenerator.generate();
        ConfirmationTokenCreatedIntegrationEvent confirmationTokenCreatedIntegrationEvent = new ConfirmationTokenCreatedIntegrationEvent(
            confirmationToken.getToken(),
            confirmationToken.getUserId().getValue(),
            confirmationToken.getEmail().getValue(),
            confirmationToken.getDuration().toString(),
            confirmationToken.getExpiresAt().toString()
        );
        IntegrationEventWrapper<ConfirmationTokenCreatedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, confirmationToken.getToken(), confirmationTokenCreatedIntegrationEvent);
        integrationEventPublisher.publish(wrapper);
        
        return null;
    }
    
}

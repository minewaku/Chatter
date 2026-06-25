package com.minewaku.chatter.message.application.service.command.message;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.MessageDeletedIntegrationEvent;
import com.minewaku.chatter.message.application.port.inbound.command.message.DeleteMessageUseCase;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class DeleteMessageApplicationService implements DeleteMessageUseCase {

    private final RecipientRepository recipientRepository;
    private final MessageRepository messageRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public DeleteMessageApplicationService(
            RecipientRepository recipientRepository,
            MessageRepository messageRepository,
            UniqueStringIdGenerator uniqueStringIdGenerator,
            OutboxStore outboxStore) {

        this.recipientRepository = recipientRepository;
        this.messageRepository = messageRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public Void handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());

        //recheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found"));
    
        messageRepository.deleteByIdInChannel(command.channelId(), command.messageId());

        IntegrationEventWrapper<MessageDeletedIntegrationEvent> integrationEventWrapper = new IntegrationEventWrapper<MessageDeletedIntegrationEvent>(
            uniqueStringIdGenerator.generate(),
            command.messageId().getValue().toString(),
            new MessageDeletedIntegrationEvent(
                command.messageId().getValue(),
                command.channelId().getValue(),
                command.requesterId().getValue()
            )
        );

        integrationEventPublisher.publish(integrationEventWrapper);
        return null;
    }
}

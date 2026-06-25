package com.minewaku.chatter.message.application.service.command.message;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.MessageCreatedIntegrationEvent;
import com.minewaku.chatter.message.application.port.inbound.command.message.SendMessageUseCase;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class SendMessageApplicationService implements SendMessageUseCase {
    
    private final MessageRepository messageRepository;
    private final RecipientRepository recipientRepository;
    private final ChannelRepository channelRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public SendMessageApplicationService(
            MessageRepository messageRepository,
            RecipientRepository recipientRepository,
            ChannelRepository channelRepository,
            TimeBasedIdGenerator timeBasedIdGenerator,
            UniqueStringIdGenerator uniqueStringIdGenerator,
            OutboxStore outboxStore) {
        
        this.messageRepository = messageRepository;
        this.recipientRepository = recipientRepository;
        this.channelRepository = channelRepository;
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public Void handle(Command command) {
        channelRepository.findById(command.channelId())
            .orElseThrow(() -> new EntityNotFoundException("Channel not found"));

        RecipientId recipientId = new RecipientId(command.guildId(), command.senderId());
        //recheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        if(command.replyId() != null) {
            messageRepository.findByIdInChannel(command.channelId(), command.replyId())
                .orElseThrow(() -> new EntityNotFoundException("Reply message not found"));
        }

        MessageId messageId = new MessageId(timeBasedIdGenerator.generate());
        Message message = Message.createNew(
            messageId,
            command.channelId(),
            command.senderId(),
            command.replyId(),
            command.content(),
            new ArrayList<>()
        );

        messageRepository.save(message);

        IntegrationEventWrapper<MessageCreatedIntegrationEvent> integrationEventWrapper = new IntegrationEventWrapper<MessageCreatedIntegrationEvent>(
            uniqueStringIdGenerator.generate(),
            messageId.getValue().toString(),
            new MessageCreatedIntegrationEvent(
                messageId.getValue(),
                command.guildId().getValue(),
                command.channelId().getValue(),
                command.senderId().getValue(),
                command.replyId() != null ? command.replyId().getValue() : null,
                command.content(),
                message.getTimestamp()
            )
        );

        integrationEventPublisher.publish(integrationEventWrapper);
        return null;
    }
}

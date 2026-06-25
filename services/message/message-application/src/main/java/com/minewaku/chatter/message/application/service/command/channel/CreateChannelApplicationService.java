package com.minewaku.chatter.message.application.service.command.channel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.ChannelCreatedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.port.inbound.command.channel.CreateChannelUseCase;
import com.minewaku.chatter.message.domain.model.channel.model.Channel;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class CreateChannelApplicationService implements CreateChannelUseCase {
    
    private final ChannelRepository channelRepository;
    private final RecipientRepository recipientRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    @Autowired
    public CreateChannelApplicationService(
        ChannelRepository channelRepository,
        RecipientRepository recipientRepository,
        TimeBasedIdGenerator timeBasedIdGenerator,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        OutboxStore outboxStore
    ) {
        this.channelRepository = channelRepository;
        this.recipientRepository = recipientRepository;
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public Void handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());

        //recheck permission
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester is not a member of the guild"));

        ChannelId channelId = new ChannelId(timeBasedIdGenerator.generate());
        Channel channel = Channel.createNew(
            channelId,
            command.guildId(),  
            command.name(), 
            command.description()
        );

        IntegrationEventWrapper<ChannelCreatedIntegrationEvent> eventWrapper = new IntegrationEventWrapper<>(
            uniqueStringIdGenerator.generate(),
            channelId.getValue().toString(),
            new ChannelCreatedIntegrationEvent(channelId.getValue(), command.guildId().getValue(), command.name(), command.description())
        );

        channelRepository.save(channel);
        integrationEventPublisher.publish(eventWrapper);
        return null;
    }
}

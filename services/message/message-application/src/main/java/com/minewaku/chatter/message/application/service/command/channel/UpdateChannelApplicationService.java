package com.minewaku.chatter.message.application.service.command.channel;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.ChannelUpdatedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.port.inbound.command.channel.UpdateChannelUseCase;
import com.minewaku.chatter.message.domain.model.channel.model.Channel;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class UpdateChannelApplicationService implements UpdateChannelUseCase {
    
    private final ChannelRepository channelRepository;
    private final RecipientRepository recipientRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public UpdateChannelApplicationService(
        ChannelRepository channelRepository,
        RecipientRepository recipientRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        OutboxStore outboxStore
    ) {
        this.channelRepository = channelRepository;
        this.recipientRepository = recipientRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    public Void handle(Command command) {
        Channel channel = channelRepository.findById(command.channelId())
            .orElseThrow(() -> new RuntimeException("Channel not found"));

        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new RuntimeException("Requester is not a member of the guild"));

        if (channel.updateInfo(command.name(), command.description())) {
            channelRepository.save(channel);

            IntegrationEventWrapper<ChannelUpdatedIntegrationEvent> eventWrapper = new IntegrationEventWrapper<>(
                uniqueStringIdGenerator.generate(),
                command.channelId().getValue().toString(),
                new ChannelUpdatedIntegrationEvent(command.channelId().getValue(), command.guildId().getValue(), command.name(), command.description())
            );

            integrationEventPublisher.publish(eventWrapper);
        }

        return null;
    }
}

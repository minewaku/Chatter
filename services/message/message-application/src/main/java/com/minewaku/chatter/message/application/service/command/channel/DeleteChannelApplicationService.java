package com.minewaku.chatter.message.application.service.command.channel;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.ChannelDeletedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.port.inbound.command.channel.DeleteChannelUseCase;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class DeleteChannelApplicationService implements DeleteChannelUseCase {
    
    private final ChannelRepository channelRepository;
    private final RecipientRepository recipientRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public DeleteChannelApplicationService(
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

    @Override
    public Void handle(Command command) {

        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());
        //recheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester is not a member of the guild"));

        channelRepository.deleteById(command.channelId());

        IntegrationEventWrapper<ChannelDeletedIntegrationEvent> eventWrapper = new IntegrationEventWrapper<>(
            uniqueStringIdGenerator.generate(),
            command.channelId().getValue().toString(),
            new ChannelDeletedIntegrationEvent(command.channelId().getValue(), command.guildId().getValue())
        );
        integrationEventPublisher.publish(eventWrapper);

        //recheck: unsubscribe all socket
        return null;
    }
}

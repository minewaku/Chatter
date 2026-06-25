package com.minewaku.chatter.message.application.service.command.guild;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.GuildDeletedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.port.inbound.command.guild.DeleteGuildUseCase;
import com.minewaku.chatter.message.domain.model.guild.repository.GuildRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class DeleteGuildApplicationService implements DeleteGuildUseCase {
    
    private final GuildRepository guildRepository;
    private final RecipientRepository recipientRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public DeleteGuildApplicationService(
        GuildRepository guildRepository,
        RecipientRepository recipientRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        OutboxStore outboxStore
    ) {
        this.guildRepository = guildRepository;
        this.recipientRepository = recipientRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public Void handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());

        //reheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        guildRepository.deleteById(command.guildId());

        IntegrationEventWrapper<GuildDeletedIntegrationEvent> eventWrapper = new IntegrationEventWrapper<GuildDeletedIntegrationEvent>(
            uniqueStringIdGenerator.generate(),
            command.guildId().getValue().toString(),
            new GuildDeletedIntegrationEvent(command.guildId().getValue())
        );

        integrationEventPublisher.publish(eventWrapper);
        return null;
    }
}

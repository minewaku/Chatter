package com.minewaku.chatter.message.application.service.command.guild;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.port.inbound.command.guild.CreateGuildUseCase;
import com.minewaku.chatter.message.domain.model.guild.model.Guild;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.guild.repository.GuildRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.Recipient;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.TimeBasedIdGenerator;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CreateGuildApplicationService implements CreateGuildUseCase {
    
    private final GuildRepository channelRepository;
    private final RecipientRepository recipientRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;

    @Override
    @Transactional
    public Void handle(Command command) {
        
        GuildId guildId = new GuildId(timeBasedIdGenerator.generate());
        RecipientId creatorRecipientId = new RecipientId(guildId, command.requesterId());
        Recipient creatorRecipient = Recipient.createNew(creatorRecipientId);

        Guild channel = Guild.createNew(
            guildId, 
            command.requesterId(),
            command.name(), 
            command.description()
        );

        channelRepository.save(channel);
        recipientRepository.save(creatorRecipient);

        return null;
    }
}

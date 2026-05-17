package com.minewaku.chatter.message.application.service.command.guild;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.guild.UpdateGuildUseCase;
import com.minewaku.chatter.message.domain.model.guild.model.Guild;
import com.minewaku.chatter.message.domain.model.guild.repository.GuildRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UpdateGuildApplicationService implements UpdateGuildUseCase {

    private final GuildRepository repository;
    private final RecipientRepository recipientRepository;

    @Override
    public Void handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());

        //recheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        Guild guild = repository.findById(command.guildId())
            .orElseThrow(() -> new EntityNotFoundException("Guild not found"));

        if (guild.updateInfo(command.name(), command.description())) {
            repository.save(guild);
        }
        return null;
    }
    
}

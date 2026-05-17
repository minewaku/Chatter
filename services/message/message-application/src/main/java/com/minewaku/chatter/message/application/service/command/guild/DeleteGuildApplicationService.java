package com.minewaku.chatter.message.application.service.command.guild;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.guild.DeleteGuildUseCase;
import com.minewaku.chatter.message.domain.model.guild.repository.GuildRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DeleteGuildApplicationService implements DeleteGuildUseCase {
    
    private final GuildRepository guildRepository;
    private final RecipientRepository recipientRepository;

    @Override
    public Void handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());

        //reheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        guildRepository.deleteById(command.guildId());
        return null;
    }
}

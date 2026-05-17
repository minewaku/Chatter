package com.minewaku.chatter.message.application.service.command.recipient;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.recipient.RemoveRecipientUseCase;
import com.minewaku.chatter.message.domain.model.guild.model.Guild;
import com.minewaku.chatter.message.domain.model.guild.repository.GuildRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class RemoveRecipientApplicationService implements RemoveRecipientUseCase {

    private final GuildRepository guildRepository;
    private final RecipientRepository recipientRepository;

    @Override
    public Void handle(Command command) {
        Guild guild = guildRepository.findById(command.guildId())
            .orElseThrow(() -> new EntityNotFoundException("Guild not found"));
        
        RecipientId requesterRecipientId = new RecipientId(guild.getId(), command.userId());

        //RECHECK: IMPLEMENTS PERMISSION HERE
        // Recipient requester = recipientRepository.findById(requesterRecipientId)
        //     .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        recipientRepository.findById(requesterRecipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        RecipientId targetRecipientId = new RecipientId(guild.getId(), command.userId());

        recipientRepository.deleteById(targetRecipientId);
        return null;
    }
    
}

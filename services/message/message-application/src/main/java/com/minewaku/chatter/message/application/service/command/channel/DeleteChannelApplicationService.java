package com.minewaku.chatter.message.application.service.command.channel;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.channel.DeleteChannelUseCase;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DeleteChannelApplicationService implements DeleteChannelUseCase {
    
    private final ChannelRepository channelRepository;
    private final RecipientRepository recipientRepository;

    @Override
    public Void handle(Command command) {

        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());
        //recheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester is not a member of the guild"));

        channelRepository.deleteById(command.channelId());
        return null;
    }
}

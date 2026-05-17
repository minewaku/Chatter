package com.minewaku.chatter.message.application.service.command.channel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.channel.CreateChannelUseCase;
import com.minewaku.chatter.message.domain.model.channel.model.Channel;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.TimeBasedIdGenerator;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CreateChannelApplicationService implements CreateChannelUseCase {
    
    private final ChannelRepository channelRepository;
    private final RecipientRepository recipientRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;

    @Override
    @Transactional
    public Void handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());

        //recheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester is not a member of the guild"));

        ChannelId channelId = new ChannelId(timeBasedIdGenerator.generate());
        Channel channel = Channel.createNew(
            channelId,
            command.guildId(),  
            command.name(), 
            command.description()
        );

        channelRepository.save(channel);
        return null;
    }
}

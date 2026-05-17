package com.minewaku.chatter.message.application.service.command.channel;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.port.inbound.command.channel.UpdateChannelUseCase;
import com.minewaku.chatter.message.domain.model.channel.model.Channel;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UpdateChannelApplicationService implements UpdateChannelUseCase {
    
    private final ChannelRepository channelRepository;
    private final RecipientRepository recipientRepository;

    public Void handle(Command command) {
        Channel channel = channelRepository.findById(command.channelId())
            .orElseThrow(() -> new RuntimeException("Channel not found"));

        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new RuntimeException("Requester is not a member of the guild"));

        if (channel.updateInfo(command.name(), command.description())) {
            channelRepository.save(channel);
        }

        return null;
    }
}

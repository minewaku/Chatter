package com.minewaku.chatter.message.application.service.query;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.port.inbound.query.CheckChannelAccessUseCase;
import com.minewaku.chatter.message.domain.model.channel.model.Channel;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CheckChannelAccessApplicationService implements CheckChannelAccessUseCase {

    private final ChannelRepository channelRepository;
    private final RecipientRepository recipientRepository;

    @Override
    public Boolean handle(Command command) {
        Channel channel = channelRepository.findById(command.channelId())
                .orElseThrow(() -> new IllegalArgumentException("Channel not found with id: " + command.channelId()));

        RecipientId recipientId = new RecipientId(channel.getGuildId(), command.userId());
        return recipientRepository.existsById(recipientId);
    }
}

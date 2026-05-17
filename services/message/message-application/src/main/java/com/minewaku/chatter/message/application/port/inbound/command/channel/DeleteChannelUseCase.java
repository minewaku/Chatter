package com.minewaku.chatter.message.application.port.inbound.command.channel;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface DeleteChannelUseCase extends UseCaseHandler<DeleteChannelUseCase.Command, Void> {
    
    record Command(
        GuildId guildId,
        ChannelId channelId,
        UserId requesterId
    ) {}
}

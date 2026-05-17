package com.minewaku.chatter.message.application.port.inbound.command.message;


import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface DeleteMessageUseCase extends UseCaseHandler<DeleteMessageUseCase.Command, Void> {
    
    public record Command(
        GuildId guildId,
        ChannelId channelId,
        MessageId messageId,
        UserId requesterId
    ) {}
}

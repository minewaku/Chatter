package com.minewaku.chatter.message.application.port.inbound.command.message;

import java.util.List;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.asset.model.Asset;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface SendMessageUseCase extends UseCaseHandler<SendMessageUseCase.Command, Void> {
    
    public record Command(
        GuildId guildId,
        ChannelId channelId,
        UserId senderId,
        MessageId replyId,
        String content,
        List<Asset> attachments
    ) {}
}

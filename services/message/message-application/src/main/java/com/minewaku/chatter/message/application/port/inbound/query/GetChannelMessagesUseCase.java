package com.minewaku.chatter.message.application.port.inbound.query;

import java.util.List;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.application.port.outbound.query.model.MessageReadModel;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

import lombok.NonNull;
public interface GetChannelMessagesUseCase extends UseCaseHandler<GetChannelMessagesUseCase.Command, List<MessageReadModel>> {

    public record Command(
        @NonNull GuildId guildId,
        @NonNull ChannelId channelId,
        @NonNull UserId requesterId,
        MessageId around,
        MessageId before,
        MessageId after,
        int limit
    ) {}
}

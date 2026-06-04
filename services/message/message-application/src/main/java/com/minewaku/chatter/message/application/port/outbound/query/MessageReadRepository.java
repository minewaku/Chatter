package com.minewaku.chatter.message.application.port.outbound.query;

import java.util.List;

import com.minewaku.chatter.message.application.port.outbound.query.model.MessageReadModel;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface MessageReadRepository {
    
    List<MessageReadModel> getMessagesByChannelId(
        ChannelId channelId,
        UserId userId,
        GuildId guildId,
        MessageId around,
        MessageId before,
        MessageId after,
        int limit
    );
}

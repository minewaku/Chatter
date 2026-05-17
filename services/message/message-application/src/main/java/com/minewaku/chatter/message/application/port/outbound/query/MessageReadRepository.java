package com.minewaku.chatter.message.application.port.outbound.query;

import java.util.List;

import com.minewaku.chatter.message.application.port.outbound.query.model.MessageReadModel;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface MessageReadRepository {
    
    List<MessageReadModel> findMessagesByChannelId(
        ChannelId channelId,
        UserId recipientId,
        MessageId around,
        MessageId before,
        MessageId after,
        int limit
    );
}

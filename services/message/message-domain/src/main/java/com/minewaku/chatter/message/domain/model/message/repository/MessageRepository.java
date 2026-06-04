package com.minewaku.chatter.message.domain.model.message.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;

public interface MessageRepository {
    Optional<Message> findByIdInChannel(ChannelId channelId, MessageId messageId);
    List<Message> findAllByIdInChannel(ChannelId channelId, Set<MessageId> messageIds);
    void save(Message message);
    void saveAll(Set<Message> messages);
    void deleteByIdInChannel(ChannelId channelId, MessageId messageId);
    List<Message> findMessagesBeforeByChannelId(ChannelId channelId, MessageId before, int limit);
    List<Message> findMessagesAfterByChannelId(ChannelId channelId, MessageId after, int limit);
    List<Message> findMessagesAroundByChannelId(ChannelId channelId, MessageId around, int limit);
    List<Message> findLatestMessagesByChannelId(ChannelId channelId, int limit);
}
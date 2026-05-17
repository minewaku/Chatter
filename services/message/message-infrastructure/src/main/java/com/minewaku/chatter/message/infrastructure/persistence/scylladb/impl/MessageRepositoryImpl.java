package com.minewaku.chatter.message.infrastructure.persistence.scylladb.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.BucketHelper;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.MessageCassandraRepository;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraKeyEntity;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.mapper.MessageCassandraMapper;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageCassandraRepository messageCassandraRepository;
    private final BucketHelper bucketHelper;
    private final MessageCassandraMapper messageMapper;

    @Override
    public Optional<Message> findByIdInChannel(ChannelId channelId, MessageId messageId) {
        int bucket = bucketHelper.calculateWeeklyBucket(messageId.getValue());
        MessageCassandraKeyEntity key = new MessageCassandraKeyEntity(channelId.getValue(), bucket, messageId.getValue());

        return messageMapper.entityToDomain(messageCassandraRepository.findById(key));
    }

    @Override
    public List<Message> findAllByIdInChannel(ChannelId channelId, Set<MessageId> messageIds) {
        List<MessageCassandraKeyEntity> keys = messageIds.stream()
            .map(messageId -> {
                int bucket = bucketHelper.calculateWeeklyBucket(messageId.getValue());
                return new MessageCassandraKeyEntity(channelId.getValue(), bucket, messageId.getValue());
            })
            .toList();

        return messageCassandraRepository.findAllById(keys).stream()
            .map(messageMapper::entityToDomain)
            .toList();
    }

    @Override
    public void save(Message message) {
        messageCassandraRepository.save(messageMapper.domainToEntity(message));
    }
    @Override
    public void saveAll(Set<Message> messages) {
        messageCassandraRepository.saveAll(messages.stream()
            .map(messageMapper::domainToEntity)
            .toList()
        );
    }
    @Override
    public void deleteByIdInChannel(ChannelId channelId, MessageId messageId) {
        int bucket = bucketHelper.calculateWeeklyBucket(messageId.getValue());
        MessageCassandraKeyEntity key = new MessageCassandraKeyEntity(channelId.getValue(), bucket, messageId.getValue());

        messageCassandraRepository.deleteById(key);
    }

    
}

package com.minewaku.chatter.message.infrastructure.persistence.scylladb.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.BucketHelper;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.ChannelBucketCassandraRepository;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.MessageCassandraRepository;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.ChannelBucketCassandraEntity;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraEntity;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraKeyEntity;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.mapper.MessageCassandraMapper;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageCassandraRepository messageCassandraRepository;
    private final MessageCassandraMapper messageMapper;

    private final ChannelBucketCassandraRepository channelBucketCassandraRepository;

    private final BucketHelper bucketHelper;


    @Override
    public Optional<Message> findByIdInChannel(ChannelId channelId, MessageId messageId) {
        int bucket = bucketHelper.calculateWeeklyBucket(messageId.getValue());
        MessageCassandraKeyEntity key = new MessageCassandraKeyEntity(channelId.getValue(), bucket, messageId.getValue());

        return messageMapper.entityToDomain(messageCassandraRepository.findById(key));
    }

    @Override
    public List<Message> findAllByIdInChannel(ChannelId channelId, Set<MessageId> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<Long>> groupedIdsByBucket = messageIds.stream()
            .map(MessageId::getValue)
            .collect(Collectors.groupingBy(
                id -> bucketHelper.calculateWeeklyBucket(id),
                Collectors.toList()
            ));

        List<MessageCassandraEntity> entities = new ArrayList<>();

        for (Map.Entry<Integer, List<Long>> entry : groupedIdsByBucket.entrySet()) {
            int bucket = entry.getKey();
            List<Long> idsInBucket = entry.getValue();

            List<MessageCassandraEntity> partitionResult = messageCassandraRepository
                .findByKeyChannelIdAndKeyBucketAndKeyIdIn(
                    channelId.getValue(), 
                    bucket, 
                    idsInBucket
                );
                
            entities.addAll(partitionResult);
        }

        return entities.stream()
            .map(messageMapper::entityToDomain)
            .toList();
    }

    @Override
    public void save(Message message) {
        MessageCassandraEntity entity = messageMapper.domainToEntity(message);
        int bucket = entity.getKey().getBucket();
        Long channelId = message.getChannelId().getValue();

        channelBucketCassandraRepository.appendBucket(channelId, bucket);
        messageCassandraRepository.save(entity);
    }

    
    public void saveAll(Set<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }


        List<MessageCassandraEntity> entities = messages.stream()
            .map(messageMapper::domainToEntity)
            .toList();

        Map<Long, Set<Integer>> uniqueChannelBuckets = new HashMap<>();
        for (MessageCassandraEntity entity : entities) {
            Long channelId = entity.getKey().getChannelId();
            int bucket = entity.getKey().getBucket();
            
            uniqueChannelBuckets
                .computeIfAbsent(channelId, k -> new HashSet<>())
                .add(bucket);
        }

        uniqueChannelBuckets.forEach((channelId, buckets) -> {
            for (Integer bucket : buckets) {
                channelBucketCassandraRepository.appendBucket(channelId, bucket);
            }
        });

        messageCassandraRepository.saveAll(entities);
    }

    @Override
    public void deleteByIdInChannel(ChannelId channelId, MessageId messageId) {
        int bucket = bucketHelper.calculateWeeklyBucket(messageId.getValue());
        MessageCassandraKeyEntity key = new MessageCassandraKeyEntity(channelId.getValue(), bucket, messageId.getValue());

        messageCassandraRepository.deleteById(key);
    }

    @Override
    //recheck: u may need to publish an event for cron job whenever an empty bucket is detected 
    public List<Message> findMessagesBeforeByChannelId(ChannelId channelId, MessageId before, int limit) {
        Long rawChannelId = channelId.getValue();
        Long rawBeforeId = before.getValue();
        
        int currentBucket = bucketHelper.calculateWeeklyBucket(rawBeforeId);
        List<MessageCassandraEntity> resultEntities = new ArrayList<>();
        int remainingLimit = limit;

        List<MessageCassandraEntity> messagesInFirstBucket = messageCassandraRepository.findMessagesBeforeByChannelId(
                rawChannelId, currentBucket, rawBeforeId, remainingLimit);
                
        resultEntities.addAll(messagesInFirstBucket);
        remainingLimit -= messagesInFirstBucket.size();

        if (remainingLimit > 0) {
            Set<Integer> allBuckets = channelBucketCassandraRepository.findById(rawChannelId)
                .map(ChannelBucketCassandraEntity::getBuckets)
                .orElse(Collections.emptySet());
            
            if (allBuckets != null && !allBuckets.isEmpty()) {
                List<Integer> pastBuckets = allBuckets.stream()
                        .filter(b -> b < currentBucket)
                        .sorted((b1, b2) -> Integer.compare(b2, b1))
                        .toList();

                for (int prevBucket : pastBuckets) {
                    if (remainingLimit <= 0) break;

                    List<MessageCassandraEntity> messagesInPrevBucket = messageCassandraRepository.findLatestMessagesInBucket(
                            rawChannelId, prevBucket, remainingLimit);
                            
                    resultEntities.addAll(messagesInPrevBucket);
                    remainingLimit -= messagesInPrevBucket.size();
                }
            }
        }

        resultEntities.sort((m1, m2) -> Long.compare(m2.getKey().getId(), m1.getKey().getId()));

        return resultEntities.stream()
                .map(messageMapper::entityToDomain)
                .toList();
    }

    @Override
    public List<Message> findMessagesAfterByChannelId(ChannelId channelId, MessageId after, int limit) {
        Long rawChannelId = channelId.getValue();
        Long rawAfterId = after.getValue();
        
        int currentBucket = bucketHelper.calculateWeeklyBucket(rawAfterId);
        List<MessageCassandraEntity> resultEntities = new ArrayList<>();
        int remainingLimit = limit;

        List<MessageCassandraEntity> messagesInFirstBucket = messageCassandraRepository.findMessagesAfterByChannelId(
                rawChannelId, currentBucket, rawAfterId, remainingLimit);
                
        resultEntities.addAll(messagesInFirstBucket);
        remainingLimit -= messagesInFirstBucket.size();

        if (remainingLimit > 0) {
            Set<Integer> allBuckets = channelBucketCassandraRepository.findById(rawChannelId)
                .map(ChannelBucketCassandraEntity::getBuckets)
                .orElse(Collections.emptySet());
            
            if (allBuckets != null && !allBuckets.isEmpty()) {
                List<Integer> futureBuckets = allBuckets.stream()
                        .filter(b -> b > currentBucket)
                        .sorted() 
                        .toList();

                for (int nextBucket : futureBuckets) {
                    if (remainingLimit <= 0) break;

                    List<MessageCassandraEntity> messagesInNextBucket = messageCassandraRepository.findOldestMessagesInBucket(
                            rawChannelId, nextBucket, remainingLimit);
                            
                    resultEntities.addAll(messagesInNextBucket);
                    remainingLimit -= messagesInNextBucket.size();
                }
            }
        }


        resultEntities.sort((m1, m2) -> Long.compare(m2.getKey().getId(), m1.getKey().getId()));
        return resultEntities.stream()
                .map(messageMapper::entityToDomain)
                .toList();
    }

    @Override
    public List<Message> findMessagesAroundByChannelId(ChannelId channelId, MessageId around, int limit) {
        if (limit <= 0) return Collections.emptyList();
        List<Message> result = new ArrayList<>();

        Optional<Message> targetMessage = findByIdInChannel(channelId, around);
        targetMessage.ifPresent(result::add);

        int remainingLimit = limit - result.size();
        if (remainingLimit <= 0) return result;

        int afterLimit = remainingLimit / 2;
        List<Message> messagesAfter = Collections.emptyList();
        if (afterLimit > 0) {
            messagesAfter = findMessagesAfterByChannelId(channelId, around, afterLimit);
            result.addAll(messagesAfter);
        }

        int beforeLimit = remainingLimit - messagesAfter.size();
        if (beforeLimit > 0) {
            List<Message> messagesBefore = findMessagesBeforeByChannelId(channelId, around, beforeLimit);
            result.addAll(messagesBefore);
        }

        result.sort((m1, m2) -> Long.compare(m2.getId().getValue(), m1.getId().getValue()));

        return result;
    }

    @Override
    public List<Message> findLatestMessagesByChannelId(ChannelId channelId, int limit) {
        
        Long rawChannelId = channelId.getValue();
        List<MessageCassandraEntity> resultEntities = new ArrayList<>();
        int remainingLimit = limit;

        Set<Integer> allBuckets = channelBucketCassandraRepository.findById(rawChannelId)
            .map(ChannelBucketCassandraEntity::getBuckets)
            .orElse(Collections.emptySet());
        
        if (allBuckets != null && !allBuckets.isEmpty()) {
            List<Integer> sortedBuckets = allBuckets.stream()
                    .sorted((b1, b2) -> Integer.compare(b2, b1))
                    .toList();

            for (int bucket : sortedBuckets) {
                if (remainingLimit <= 0) break;

                List<MessageCassandraEntity> messagesInBucket = messageCassandraRepository.findLatestMessagesInBucket(
                        rawChannelId, bucket, remainingLimit);
                        
                resultEntities.addAll(messagesInBucket);
                remainingLimit -= messagesInBucket.size();
            }
        }

        resultEntities.sort((m1, m2) -> Long.compare(m2.getKey().getId(), m1.getKey().getId()));
        return resultEntities.stream().map(messageMapper::entityToDomain).toList();
    }
}

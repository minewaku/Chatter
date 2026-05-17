package com.minewaku.chatter.message.infrastructure.persistence.scylladb.mapper;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraEntity;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraKeyEntity;

@Component
public class MessageCassandraMapper {

    // Bucket theo tháng (đơn vị milliseconds) — có thể điều chỉnh theo yêu cầu
    private static final long BUCKET_DURATION_MS = 1000L * 60 * 60 * 24 * 30;

    public MessageCassandraEntity domainToEntity(Message domain) {
        MessageCassandraKeyEntity key = buildKey(domain);

        return new MessageCassandraEntity(
            key,
            domain.getContent(),
            domain.getUserId().getValue(),
            domain.getReplyId() != null ? domain.getReplyId().getValue() : null,
            domain.getAssetHashes(),
            domain.getTimestamp().toEpochMilli()
        );
    }

    public Message entityToDomain(MessageCassandraEntity entity) {
        MessageCassandraKeyEntity key = entity.getKey();

        return Message.reconstitute(
            new MessageId(key.getMessageId()),
            new ChannelId(key.getChannelId()),
            new UserId(entity.getUserId()),
            new MessageId(entity.getReplyId()),
            entity.getAssetHashes(),
            entity.getContent(),
            toInstant(entity.getTimestamp())
        );
    }

    public Optional<Message> entityToDomain(Optional<MessageCassandraEntity> entityOptional) {
        return entityOptional.map(this::entityToDomain);
    }


    // --- Private helpers ---
    private MessageCassandraKeyEntity buildKey(Message domain) {
        long messageId = domain.getMessageId().getValue();
        long channelId = domain.getChannelId().getValue();
        int bucket = toBucket(domain.getTimestamp());

        return new MessageCassandraKeyEntity(channelId, bucket, messageId);
    }

    private int toBucket(Instant timestamp) {
        return (int) (timestamp.toEpochMilli() / BUCKET_DURATION_MS);
    }

    private Instant toInstant(Long epochMilli) {
        return Instant.ofEpochMilli(epochMilli);
    }
}
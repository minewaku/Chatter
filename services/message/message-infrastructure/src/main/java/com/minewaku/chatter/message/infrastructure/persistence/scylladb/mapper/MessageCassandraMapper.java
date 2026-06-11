package com.minewaku.chatter.message.infrastructure.persistence.scylladb.mapper;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.message.model.Attachment;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.BucketHelper;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.AttachmentCassandraEntity;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraEntity;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraKeyEntity;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class MessageCassandraMapper {

    private final BucketHelper bucketHelper;

    public MessageCassandraEntity domainToEntity(Message domain) {
        MessageCassandraKeyEntity key = buildKey(domain);

        return new MessageCassandraEntity(
            key,
            domain.getContent(),
            domain.getUserId().getValue(),
            domain.getReplyId() != null ? domain.getReplyId().getValue() : null,
            domain.getAttachments().stream()
                .map(attachment -> new AttachmentCassandraEntity(
                    attachment.getFileHash(),
                    attachment.getFilename(),
                    attachment.getContentType(),
                    attachment.getSize()))
                .toList(),
            domain.getTimestamp().toEpochMilli()
        );
    }

    public Message entityToDomain(MessageCassandraEntity entity) {
        MessageCassandraKeyEntity key = entity.getKey();

        return Message.reconstitute(
            new MessageId(key.getId()),
            new ChannelId(key.getChannelId()),
            new UserId(entity.getUserId()),
            entity.getReplyId() != null ? new MessageId(entity.getReplyId()) : null,
            entity.getContent(),
            toInstant(entity.getTimestamp()),
            entity.getAttachments().stream()
                .map(attachmentEntity -> new Attachment(
                    attachmentEntity.getFileHash(),
                    attachmentEntity.getFilename(),
                    attachmentEntity.getContentType(),
                    attachmentEntity.getSize()))
                .toList()
        );
    }

    public Optional<Message> entityToDomain(Optional<MessageCassandraEntity> entityOptional) {
        return entityOptional.map(this::entityToDomain);
    }

    // --- Private helpers ---
    private MessageCassandraKeyEntity buildKey(Message domain) {
        long messageId = domain.getId().getValue();
        long channelId = domain.getChannelId().getValue();
        int bucket = bucketHelper.calculateWeeklyBucket(domain.getTimestamp());

        return new MessageCassandraKeyEntity(channelId, bucket, messageId);
    }

    private Instant toInstant(Long epochMilli) {
        return epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }
}
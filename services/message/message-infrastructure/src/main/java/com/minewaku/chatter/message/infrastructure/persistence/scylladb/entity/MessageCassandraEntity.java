package com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Getter;
import lombok.NonNull;

@Table("message")
@Getter
public class MessageCassandraEntity {

    @PrimaryKey
    private MessageCassandraKeyEntity key;
    
    @Column("content")
    private String content;

    @Column("user_id")
    private Long userId;

    @Column("reply_id")
    private Long replyId;

    @Column("attachments")
    private List<AttachmentCassandraEntity> attachments;

    @Column("timestamp")
    private Long timestamp;

    public MessageCassandraEntity(
        @NonNull MessageCassandraKeyEntity key,
        @NonNull String content,
        @NonNull Long userId,
        Long replyId,
        List<AttachmentCassandraEntity> attachments,
        @NonNull Long timestamp
    ) {
        this.key = key;
        this.content = content;
        this.userId = userId;
        this.replyId = replyId;
        this.attachments = attachments != null ? attachments : new ArrayList<>();
        this.timestamp = timestamp;
    }
}

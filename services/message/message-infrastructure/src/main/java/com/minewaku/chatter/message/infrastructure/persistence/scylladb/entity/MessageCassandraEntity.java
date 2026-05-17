package com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity;

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

    @Column("asset_hashes")
    private List<String> assetHashes;

    @Column("timestamp")
    private Long timestamp;

    public MessageCassandraEntity(
        @NonNull MessageCassandraKeyEntity key,
        @NonNull String content,
        @NonNull Long userId,
        @NonNull Long replyId,
        @NonNull List<String> assetHashes,
        @NonNull Long timestamp
    ) {
        this.key = key;
        this.content = content;
        this.userId = userId;
        this.replyId = replyId;
        this.assetHashes = assetHashes;
        this.timestamp = timestamp;
    }
}

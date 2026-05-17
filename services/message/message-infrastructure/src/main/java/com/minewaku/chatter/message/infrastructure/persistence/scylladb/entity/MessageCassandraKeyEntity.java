package com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.Getter;
import lombok.NonNull;

@PrimaryKeyClass
@Getter
public class MessageCassandraKeyEntity implements Serializable {
    
    @PrimaryKeyColumn(name = "channel_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private Long channelId;

    @PrimaryKeyColumn(name = "bucket", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private int bucket;

    @PrimaryKeyColumn(name = "message_id", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING, ordinal = 2)
    private Long messageId; 

    public MessageCassandraKeyEntity(
            @NonNull Long channelId, 
            int bucket, 
            @NonNull Long messageId) {
                
        this.channelId = channelId;
        this.bucket = bucket;
        this.messageId = messageId;
    }
}

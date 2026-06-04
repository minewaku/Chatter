package com.minewaku.chatter.message.infrastructure.persistence.scylladb;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraEntity;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraKeyEntity;

public interface MessageCassandraRepository extends CassandraRepository<MessageCassandraEntity, MessageCassandraKeyEntity> {

    @Query("SELECT * FROM message WHERE channel_id = :channelId AND bucket = :bucket AND message_id < :before LIMIT :limit")
    List<MessageCassandraEntity> findMessagesBeforeByChannelId(Long channelId, int bucket, Long before, int limit);

    @Query("SELECT * FROM message WHERE channel_id = :channelId AND bucket = :bucket AND message_id > :after LIMIT :limit")
    List<MessageCassandraEntity> findMessagesAfterByChannelId(Long channelId, int bucket, Long after, int limit);

    @Query("SELECT * FROM message WHERE channel_id = :channelId AND bucket = :bucket ORDER BY message_id ASC LIMIT :limit")
    List<MessageCassandraEntity> findOldestMessagesInBucket(@Param("channelId") Long channelId, @Param("bucket") int bucket, @Param("limit") int limit);

    @Query("SELECT * FROM message WHERE channel_id = :channelId AND bucket = :bucket LIMIT :limit")
    List<MessageCassandraEntity> findLatestMessagesInBucket(@Param("channelId") Long channelId, @Param("bucket") int bucket, @Param("limit") int limit);
}

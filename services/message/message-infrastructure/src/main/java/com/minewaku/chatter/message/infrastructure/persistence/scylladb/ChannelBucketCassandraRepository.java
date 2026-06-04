package com.minewaku.chatter.message.infrastructure.persistence.scylladb;

import java.util.Set;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.ChannelBucketCassandraEntity;

public interface ChannelBucketCassandraRepository extends CassandraRepository<ChannelBucketCassandraEntity, Long> {
    
    @Query("UPDATE channel_bucket SET buckets = buckets + { :bucket } WHERE channel_id = :channelId")
    void appendBucket(@Param("channelId") Long channelId, @Param("bucket") Integer bucket);

    @Query("SELECT buckets FROM channel_bucket WHERE channel_id = :channelId")
    Set<Integer> findBucketsByChannelId(@Param("channelId") Long channelId);
}

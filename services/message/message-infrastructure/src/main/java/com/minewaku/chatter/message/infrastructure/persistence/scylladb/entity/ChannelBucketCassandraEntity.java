package com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Table("channel_bucket")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelBucketCassandraEntity {
    
    @PrimaryKey("channel_id")
    private Long channelId;

    @Builder.Default
    @Column("buckets")
    private Set<Integer> buckets = new HashSet<>();

    public void addBucket(Integer bucket) {
        this.buckets.add(bucket);
    }
}
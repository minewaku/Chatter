package com.minewaku.chatter.message.infrastructure.persistence.postgresql.entity;

import java.util.List;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("channel_bucket")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelBucketJdbcEntity {
    
    @Column("channel_id")
    private Long channelId;

    @Column("buckets")
    private List<Integer> buckets;

    public boolean addBucket(Integer bucket) {
        if(!buckets.contains(bucket)) {
            buckets.add(bucket);
            return true;
        }

        return false;
    }
}

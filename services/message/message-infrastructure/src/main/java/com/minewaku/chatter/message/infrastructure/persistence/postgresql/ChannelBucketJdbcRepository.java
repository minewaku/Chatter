package com.minewaku.chatter.message.infrastructure.persistence.postgresql;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.message.infrastructure.persistence.postgresql.entity.ChannelBucketJdbcEntity;

public interface ChannelBucketJdbcRepository extends ListCrudRepository<ChannelBucketJdbcEntity, Long> {
    
}

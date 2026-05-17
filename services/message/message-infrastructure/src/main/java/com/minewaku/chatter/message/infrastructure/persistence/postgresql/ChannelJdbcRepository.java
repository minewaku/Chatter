package com.minewaku.chatter.message.infrastructure.persistence.postgresql;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.message.domain.model.channel.model.Channel;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;

public interface ChannelJdbcRepository extends ListCrudRepository<Channel, ChannelId> {
    List<Channel> findByUserId(Long userId);
}

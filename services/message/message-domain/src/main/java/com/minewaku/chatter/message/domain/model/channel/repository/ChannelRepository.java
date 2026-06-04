package com.minewaku.chatter.message.domain.model.channel.repository;

import java.util.Optional;

import com.minewaku.chatter.message.domain.model.channel.model.Channel;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;

public interface ChannelRepository {
    void save(Channel channel);
    Optional<Channel> findById(ChannelId channelId);
    void deleteById(ChannelId channelId);
}

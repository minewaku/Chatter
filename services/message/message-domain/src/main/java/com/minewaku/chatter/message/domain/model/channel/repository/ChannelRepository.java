package com.minewaku.chatter.message.domain.model.channel.repository;

import java.util.List;
import java.util.Optional;

import com.minewaku.chatter.message.domain.model.channel.model.Channel;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface ChannelRepository {
    void save(Channel channel);
    Optional<Channel> findById(ChannelId channelId);
    void deleteById(ChannelId channelId);
    List<Channel> findByUserId(UserId userId);
}

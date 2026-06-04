package com.minewaku.chatter.message.infrastructure.persistence.postgresql.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.domain.model.channel.model.Channel;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.infrastructure.persistence.postgresql.ChannelJdbcRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ChannelRepositoryImpl implements ChannelRepository{
    
    private final ChannelJdbcRepository channelJdbcRepository;

    @Override
    public void save(Channel channel) {
        channelJdbcRepository.save(channel);
    }

    @Override
    public Optional<Channel> findById(ChannelId channelId) {
        return channelJdbcRepository.findById(channelId);
    }

    @Override
    public void deleteById(ChannelId channelId) {
        channelJdbcRepository.deleteById(channelId);
    }
}

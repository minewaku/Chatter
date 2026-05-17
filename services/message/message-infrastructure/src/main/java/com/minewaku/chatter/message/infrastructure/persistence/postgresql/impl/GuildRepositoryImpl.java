package com.minewaku.chatter.message.infrastructure.persistence.postgresql.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.domain.model.guild.model.Guild;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.guild.repository.GuildRepository;
import com.minewaku.chatter.message.infrastructure.persistence.postgresql.GuildJdbcRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class GuildRepositoryImpl implements GuildRepository {

    private final GuildJdbcRepository guildJdbcRepository;

    @Override
    public void save(Guild guild) {
        guildJdbcRepository.save(guild);
    }

    @Override
    public void delete(Guild guild) { 
        guildJdbcRepository.delete(guild);
    }

    @Override
    public void deleteById(GuildId guildId) {
        guildJdbcRepository.deleteById(guildId);
    }

    @Override
    public Optional<Guild> findById(GuildId guildId) {
        return guildJdbcRepository.findById(guildId);
    }
    
}

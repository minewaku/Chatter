package com.minewaku.chatter.message.domain.model.guild.repository;

import java.util.Optional;

import com.minewaku.chatter.message.domain.model.guild.model.Guild;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;

public interface GuildRepository {
    void save(Guild guild);
    void delete(Guild guild);
    void deleteById(GuildId guildId);
    Optional<Guild> findById(GuildId guildId);
}

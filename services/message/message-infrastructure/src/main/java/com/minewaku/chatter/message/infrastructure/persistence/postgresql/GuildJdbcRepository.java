package com.minewaku.chatter.message.infrastructure.persistence.postgresql;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.message.domain.model.guild.model.Guild;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;

public interface GuildJdbcRepository extends ListCrudRepository<Guild, GuildId> {
    
}

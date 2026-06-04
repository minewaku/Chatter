package com.minewaku.chatter.message.domain.model.recipient.model;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;

import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.sharedkernel.value.Id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class RecipientId implements Id {
    
    @Column("user_id")
    private final UserId userId;

    @Column("guild_id")
    private final GuildId guildId;  

    @PersistenceCreator
    public RecipientId(
            @NonNull GuildId guildId,
            @NonNull UserId userId) {

        this.guildId = guildId;
        this.userId = userId;
    }
}
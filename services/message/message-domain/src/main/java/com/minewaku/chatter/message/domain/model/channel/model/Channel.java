package com.minewaku.chatter.message.domain.model.channel.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.sharedkernel.value.BaseEntity;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Table("channel")
@ToString
public class Channel extends BaseEntity<ChannelId> {
    
    @Id
    @Embedded.Nullable
    private ChannelId id;

    @Embedded.Nullable
    private GuildId guildId;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @PersistenceCreator
    private Channel(
            @NonNull ChannelId id, 
            @NonNull GuildId guildId, 
            String name, 
            String description) {
        
        this.id = id;
        this.guildId = guildId;
        this.name = name;
        this.description = description;
    }

    public static Channel createNew(
            @NonNull ChannelId channelId, 
            @NonNull GuildId guildId, 
            String name, 
            String description) {

        return new Channel(channelId, guildId, name, description);
    }

    public boolean updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
        return true;
    }
}

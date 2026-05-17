package com.minewaku.chatter.message.domain.model.guild.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.message.domain.model.recipient.model.UserId;
import com.minewaku.chatter.message.domain.sharedkernel.value.BaseEntity;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Table("guild")
@ToString
public class Guild extends BaseEntity<GuildId> {
    
    @Id
    @Embedded.Nullable
    private GuildId id;

    @Embedded.Nullable
    private UserId ownerId;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("icon_hash")
    private String iconHash;

    @PersistenceCreator
    private Guild(
            @NonNull GuildId id, 
            @NonNull UserId ownerId, 
            @NonNull String name, 
            String description,
            String iconHash) {
        
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.iconHash = iconHash;
    }

    public static Guild createNew(
            @NonNull GuildId guildId, 
            @NonNull UserId creatorId, 
            @NonNull String name, 
            String description) {

        return new Guild(guildId, creatorId, name, description, null);
    }

    public boolean updateInfo(String name, String description) {
        boolean result = false;
        if(name != null && !this.name.equals(name)) {
            this.name = name;
            result = true;
        }
        if(description != null && !this.description.equals(description)) {
            this.description = description;
            result = true;
        }

        return result;
    }

    public boolean changeIcon(String iconHash) {
        if(this.iconHash.equals(iconHash)) {
            return false;
        }
        this.iconHash = iconHash;
        return true;
    }
}


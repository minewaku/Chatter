package com.minewaku.chatter.message.domain.model.guild.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.message.domain.model.recipient.model.UserId;
import com.minewaku.chatter.message.domain.sharedkernel.exception.DomainValidationException;
import com.minewaku.chatter.message.domain.sharedkernel.value.BaseEntity;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Table("guild")
@ToString
public class Guild extends BaseEntity<GuildId> {
    
    @Id
    @Column("id")
    private GuildId id;

    private UserId userId;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("icon_hash")
    private String iconHash;

    @Version
    private Integer version;

    @PersistenceCreator
    private Guild(
            @NonNull GuildId id, 
            @NonNull UserId userId, 
            @NonNull String name, 
            String description,
            String iconHash,
            Integer version) {
        
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.iconHash = iconHash;
        this.version = version;
    }

    public static Guild createNew(
            @NonNull GuildId id, 
            @NonNull UserId userId, 
            @NonNull String name, 
            String description) {

        return new Guild(id, userId, name, description, null, null);
    }

    public boolean updateInfo(String name, String description) {
        boolean result = false;
        if(name == null || name.trim().isEmpty()) throw new DomainValidationException("name must not be empty or null");

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


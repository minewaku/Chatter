package com.minewaku.chatter.message.domain.model.guild.event;

import com.minewaku.chatter.message.domain.model.asset.model.Namespace;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;

@Getter
public class GuildIconReplacedDomainEvent extends DomainEvent {

    private final String namespace = Namespace.GUILD_ICON.name();
    private final Long guildId;
    private final String oldHashFile;
    private final String newHashFile;

    public GuildIconReplacedDomainEvent(
            Long guildId,
            String oldHashFile,
            String newHashFile) {

        this.guildId = guildId;
        this.oldHashFile = oldHashFile;
        this.newHashFile = newHashFile;
    }
}

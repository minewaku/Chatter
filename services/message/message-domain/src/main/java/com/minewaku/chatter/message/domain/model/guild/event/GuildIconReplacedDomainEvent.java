package com.minewaku.chatter.message.domain.model.guild.event;

import com.minewaku.chatter.message.domain.model.asset.model.Namespace;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;

@Getter
public class GuildIconReplacedDomainEvent extends DomainEvent {

    private final String namespace = Namespace.GUILD_ICON.name();
    private final String oldHashFile;
    private final String newHashFile;

    public GuildIconReplacedDomainEvent(
            String oldHashFile,
            String newHashFile) {

        this.oldHashFile = oldHashFile;
        this.newHashFile = newHashFile;
    }
}
